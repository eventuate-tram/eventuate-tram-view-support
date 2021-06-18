package io.eventuate.tram.viewsupport.rebuild;

import io.eventuate.common.id.IdGenerator;
import io.eventuate.common.jdbc.EventuateCommonJdbcOperations;
import io.eventuate.common.jdbc.EventuateSchema;
import io.eventuate.common.json.mapper.JSonMapper;
import io.eventuate.messaging.kafka.producer.EventuateKafkaProducer;
import io.eventuate.tram.events.common.EventMessageHeaders;
import io.eventuate.tram.messaging.common.Message;
import io.eventuate.tram.messaging.producer.MessageBuilder;
import io.eventuate.tram.messaging.producer.HttpDateHeaderFormatUtil;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DomainSnapshotExportService<T> {
  protected PagingAndSortingRepository<T, Long> domainRepository;

  private EventuateSchema eventuateSchema;
  private EventuateCommonJdbcOperations eventuateCommonJdbcOperations;
  private EventuateKafkaProducer eventuateKafkaProducer;
  private DBLockService dbLockService;
  private IdGenerator idGenerator;

  private Class<T> domainClass;
  private Function<T, DomainEventWithEntityId> domainEntityToDomainEventConverter;
  private DBLockService.TableSpec domainTableSpec;
  private int iterationPageSize;
  private String cdcServiceUrl;
  private String cdcStatusServiceEndPoint;
  private String readerName;
  private int maxIterationsToCheckCdcProcessing;
  private int timeoutBetweenCdcProcessingCheckingIterationsInMilliseconds;
  private RestTemplate restTemplate = new RestTemplate();

  public DomainSnapshotExportService(EventuateSchema eventuateSchema,
                                     EventuateCommonJdbcOperations eventuateCommonJdbcOperations,
                                     EventuateKafkaProducer eventuateKafkaProducer,
                                     DBLockService dbLockService,
                                     IdGenerator idGenerator,
                                     Class<T> domainClass,
                                     PagingAndSortingRepository<T, Long> domainRepository,
                                     Function<T, DomainEventWithEntityId> domainEntityToDomainEventConverter,
                                     DBLockService.TableSpec domainTableSpec,
                                     int iterationPageSize,
                                     String cdcServiceUrl,
                                     String cdcStatusServiceEndPoint,
                                     String readerName,
                                     int maxIterationsToCheckCdcProcessing,
                                     int timeoutBetweenCdcProcessingCheckingIterationsInMilliseconds) {

    this.eventuateSchema = eventuateSchema;
    this.eventuateCommonJdbcOperations = eventuateCommonJdbcOperations;
    this.eventuateKafkaProducer = eventuateKafkaProducer;
    this.dbLockService = dbLockService;
    this.idGenerator = idGenerator;
    this.domainClass = domainClass;
    this.domainRepository = domainRepository;
    this.domainEntityToDomainEventConverter = domainEntityToDomainEventConverter;
    this.domainTableSpec = domainTableSpec;
    this.iterationPageSize = iterationPageSize;
    this.cdcServiceUrl = cdcServiceUrl;
    this.cdcStatusServiceEndPoint = cdcStatusServiceEndPoint;
    this.readerName = readerName;
    this.maxIterationsToCheckCdcProcessing = maxIterationsToCheckCdcProcessing;
    this.timeoutBetweenCdcProcessingCheckingIterationsInMilliseconds = timeoutBetweenCdcProcessingCheckingIterationsInMilliseconds;
  }

  public List<SnapshotMetadata> exportSnapshots() {

    Map<DBLockService.TableSpec, DBLockService.LockType> messageTableLock = Collections
            .singletonMap(new DBLockService.TableSpec(eventuateSchema.qualifyTable("message")), DBLockService.LockType.WRITE);

    DBLockService.LockSpecification lockSpecification = new DBLockService.LockSpecification(domainTableSpec,
            DBLockService.LockType.READ, messageTableLock);

    return dbLockService.withLockedTables(lockSpecification, this::publishSnapshots);
  }

  private List<SnapshotMetadata> publishSnapshots() {
    waitUntilCdcProcessingFinished(readerName);

    ViewSupportIdGenerator viewSupportIdGenerator =
            new ViewSupportIdGenerator(eventuateSchema, eventuateCommonJdbcOperations, idGenerator);

    List<SnapshotMetadata> snapshotMetadata = publishSnapshotEvents(viewSupportIdGenerator);

    iterateOverAllDomainEntities(entity -> publishDomainEntity(entity, viewSupportIdGenerator));

    return snapshotMetadata;
  }

  private void waitUntilCdcProcessingFinished(String readerName) {
    for (int i = 1; i <= maxIterationsToCheckCdcProcessing; i++) {
      if (getCdcProcessingStatus(readerName).isCdcProcessingFinished()) {
        break;
      } else if (i == maxIterationsToCheckCdcProcessing) {
        throw new RuntimeException("Cdc message processing was not finished in time.");
      } else {
        try {
          Thread.sleep(timeoutBetweenCdcProcessingCheckingIterationsInMilliseconds);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  private CdcProcessingStatus getCdcProcessingStatus(String readerName) {
    return restTemplate
            .getForObject(String.format("%s/%s?readerName=%s", cdcServiceUrl, cdcStatusServiceEndPoint, readerName),
                    CdcProcessingStatus.class);
  }

  void iterateOverAllDomainEntities(Consumer<T> callback) {
    for (int i = 0; ; i++) {
      Page<T> page = domainRepository.findAll(PageRequest.of(i, iterationPageSize));
      page.forEach(callback);
      if (page.isLast()) {
        break;
      }
    }
  }

  private RecordMetadata getRecordMetadataFromFuture(CompletableFuture<?> completableFuture) {
    try {
      return (RecordMetadata)completableFuture.get();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  private List<SnapshotMetadata> publishSnapshotEvents(ViewSupportIdGenerator viewSupportIdGenerator) {
    List<CompletableFuture<?>> metadata = new ArrayList<>();

    int kafkaPartitions = eventuateKafkaProducer.partitionsFor(domainClass.getName()).size();

    for (int i = 0; i < kafkaPartitions; i++) {
      Message message = MessageBuilder
              .withPayload("")
              .withHeader(Message.ID, viewSupportIdGenerator.generateId())
              .withHeader(EventMessageHeaders.AGGREGATE_ID, "")
              .withHeader(EventMessageHeaders.AGGREGATE_TYPE, domainClass.getName())
              .withHeader(EventMessageHeaders.EVENT_TYPE, SnapshotOffsetEvent.class.getName())
              .withHeader(Message.DESTINATION, domainClass.getName())
              .withHeader(Message.DATE, HttpDateHeaderFormatUtil.nowAsHttpDateString())
              .build();

      CompletableFuture<?> recordInfo = eventuateKafkaProducer.send(domainClass.getName(),
              i,
              "",
              JSonMapper.toJson(message));

      metadata.add(recordInfo);
    }

    return metadata
            .stream()
            .map(meta -> {
              RecordMetadata recordMetadata = getRecordMetadataFromFuture(meta);
              return new SnapshotMetadata(recordMetadata.topic(), recordMetadata.partition(), recordMetadata.offset());
            })
            .collect(Collectors.toList());
  }

  private void publishDomainEntity(T domainEntity, ViewSupportIdGenerator viewSupportIdGenerator) {
    DomainEventWithEntityId domainEventWithEntityId = domainEntityToDomainEventConverter.apply(domainEntity);

    Message message = MessageBuilder
            .withPayload(JSonMapper.toJson(domainEventWithEntityId.getDomainEvent()))
            .withHeader(Message.ID, viewSupportIdGenerator.generateId())
            .withHeader(EventMessageHeaders.AGGREGATE_ID, domainEventWithEntityId.getEntityId().toString())
            .withHeader(EventMessageHeaders.AGGREGATE_TYPE, domainClass.getName())
            .withHeader(EventMessageHeaders.EVENT_TYPE, domainEventWithEntityId.getDomainEvent().getClass().getName())
            .withHeader(Message.DESTINATION,domainClass.getName())
            .withHeader(Message.DATE, HttpDateHeaderFormatUtil.nowAsHttpDateString())
            .build();

    eventuateKafkaProducer.send(domainClass.getName(),
            domainEventWithEntityId.getEntityId().toString(),
            JSonMapper.toJson(message));
  }
}
