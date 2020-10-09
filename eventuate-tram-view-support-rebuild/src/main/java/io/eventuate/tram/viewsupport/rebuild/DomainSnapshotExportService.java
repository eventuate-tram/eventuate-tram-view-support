package io.eventuate.tram.viewsupport.rebuild;

import io.eventuate.common.id.IdGenerator;
import io.eventuate.common.id.Int128;
import io.eventuate.common.jdbc.EventuateCommonJdbcOperations;
import io.eventuate.common.jdbc.EventuateSchema;
import io.eventuate.common.json.mapper.JSonMapper;
import io.eventuate.messaging.kafka.producer.EventuateKafkaProducer;
import io.eventuate.tram.events.common.EventMessageHeaders;
import io.eventuate.tram.messaging.common.Message;
import io.eventuate.tram.messaging.producer.MessageBuilder;
import io.eventuate.tram.messaging.producer.common.HttpDateHeaderFormatUtil;
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

  public List<TopicPartitionOffsetMessageId> exportSnapshots() {

    Map<DBLockService.TableSpec, DBLockService.LockType> messageTableLock = Collections
            .singletonMap(new DBLockService.TableSpec(eventuateSchema.qualifyTable("message")), DBLockService.LockType.WRITE);

    DBLockService.LockSpecification lockSpecification = new DBLockService.LockSpecification(domainTableSpec,
            DBLockService.LockType.READ, messageTableLock);

    return dbLockService.withLockedTables(lockSpecification, this::publishSnapshots);
  }

  private List<TopicPartitionOffsetMessageId> publishSnapshots() {
    waitUntilCdcProcessingFinished(readerName);
    List<TopicPartitionOffsetMessageId> topicPartitionOffsetMessageIds = publishSnapshotEvents();

    Map<Integer, TopicPartitionOffsetMessageId> topicPartitionOffsetMessageIdsSortedByPartition =
            sortByPartition(topicPartitionOffsetMessageIds);

    iterateOverAllDomainEntities(entity -> publishDomainEntity(entity, topicPartitionOffsetMessageIdsSortedByPartition));
    return topicPartitionOffsetMessageIds;
  }

  private Map<Integer, TopicPartitionOffsetMessageId> sortByPartition(List<TopicPartitionOffsetMessageId> topicPartitionOffsetMessageIds) {
    Map<Integer, TopicPartitionOffsetMessageId> sorted = new HashMap<>();
    topicPartitionOffsetMessageIds.forEach(tpomi -> sorted.put(tpomi.getPartition(), tpomi));
    return sorted;
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

  private List<TopicPartitionOffsetMessageId> publishSnapshotEvents() {
    Map<String, CompletableFuture<?>> metadata = new HashMap<>();

    int kafkaPartitions = eventuateKafkaProducer.partitionsFor(domainClass.getName()).size();

    for (int i = 0; i < kafkaPartitions; i++) {
      String anchorMessageId = generateAnchorMessageId();

      Message message = MessageBuilder
              .withPayload("")
              .withHeader(Message.ID, anchorMessageId)
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

      metadata.put(anchorMessageId, recordInfo);
    }

    return metadata
            .entrySet()
            .stream()
            .map(e -> {
              RecordMetadata recordMetadata = getRecordMetadataFromFuture(e.getValue());
              return new TopicPartitionOffsetMessageId(recordMetadata.topic(), recordMetadata.partition(), recordMetadata.offset(), e.getKey());
            })
            .collect(Collectors.toList());
  }

  private void publishDomainEntity(T domainEntity, Map<Integer, TopicPartitionOffsetMessageId> topicPartitionOffsetMessageIds) {
    DomainEventWithEntityId domainEventWithEntityId = domainEntityToDomainEventConverter.apply(domainEntity);

    int partition = eventuateKafkaProducer.partitionFor(domainClass.getName(), domainEventWithEntityId.getEntityId().toString());

    TopicPartitionOffsetMessageId topicPartitionOffsetMessageId = topicPartitionOffsetMessageIds.get(partition);

    String messageId = idGenerator
            .incrementIdIfPossible(Int128.fromString(topicPartitionOffsetMessageId.getDatabaseId()))
            .map(Int128::asString)
            .orElseGet(this::generateAnchorMessageId);

    topicPartitionOffsetMessageId.setDatabaseId(messageId);

    Message message = MessageBuilder
            .withPayload(JSonMapper.toJson(domainEventWithEntityId.getDomainEvent()))
            .withHeader(Message.ID, messageId)
            .withHeader(EventMessageHeaders.AGGREGATE_ID, domainEventWithEntityId.getEntityId().toString())
            .withHeader(EventMessageHeaders.AGGREGATE_TYPE, domainClass.getName())
            .withHeader(EventMessageHeaders.EVENT_TYPE, domainEventWithEntityId.getDomainEvent().getClass().getName())
            .build();

    eventuateKafkaProducer.send(domainClass.getName(),
            domainEventWithEntityId.getEntityId().toString(),
            JSonMapper.toJson(message));
  }

  private String generateAnchorMessageId() {
    return eventuateCommonJdbcOperations.insertIntoMessageTable(idGenerator,
              "", "", Collections.emptyMap(), eventuateSchema, true);
  }
}
