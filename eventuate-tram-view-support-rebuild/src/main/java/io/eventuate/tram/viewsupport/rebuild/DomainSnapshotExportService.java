package io.eventuate.tram.viewsupport.rebuild;

import com.google.common.collect.ImmutableMap;
import io.eventuate.javaclient.commonimpl.JSonMapper;
import io.eventuate.javaclient.spring.jdbc.IdGenerator;
import io.eventuate.local.java.kafka.producer.EventuateKafkaProducer;
import io.eventuate.tram.events.common.EventMessageHeaders;
import io.eventuate.tram.events.publisher.DomainEventPublisher;
import io.eventuate.tram.messaging.common.Message;
import io.eventuate.tram.messaging.producer.MessageBuilder;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.PartitionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DomainSnapshotExportService<T> {
  private Logger logger = LoggerFactory.getLogger(getClass());

  protected PagingAndSortingRepository<T, Long> domainRepository;

  private DomainEventPublisher domainEventPublisher;
  private EventuateKafkaProducer eventuateKafkaProducer;
  private DBLockService dbLockService;
  private IdGenerator idGenerator;

  private Class<T> domainClass;
  private Function<T, DomainEventWithEntityId> domainEntityToDomainEventConverter;
  private DBLockService.TableSpec domainTableSpec;
  private int iterationPageSize;

  public DomainSnapshotExportService(DomainEventPublisher domainEventPublisher,
                                     EventuateKafkaProducer eventuateKafkaProducer,
                                     DBLockService dbLockService,
                                     IdGenerator idGenerator,
                                     Class<T> domainClass,
                                     PagingAndSortingRepository<T, Long> domainRepository,
                                     Function<T, DomainEventWithEntityId> domainEntityToDomainEventConverter,
                                     DBLockService.TableSpec domainTableSpec,
                                     int iterationPageSize) {

    this.domainEventPublisher = domainEventPublisher;
    this.eventuateKafkaProducer = eventuateKafkaProducer;
    this.dbLockService = dbLockService;
    this.idGenerator = idGenerator;
    this.domainClass = domainClass;
    this.domainRepository = domainRepository;
    this.domainEntityToDomainEventConverter = domainEntityToDomainEventConverter;
    this.domainTableSpec = domainTableSpec;
    this.iterationPageSize = iterationPageSize;
  }

  public String exportSnapshots() {
    DBLockService.LockSpecification lockSpecification = new DBLockService.LockSpecification(domainTableSpec,
            DBLockService.LockType.READ,
            ImmutableMap.of(new DBLockService.TableSpec("message"), DBLockService.LockType.WRITE));

    return dbLockService.withLockedTables(lockSpecification,
            this::publishSnapshotEventsAndCollectTopicsPartitionsOffsetsForSnapshots);
  }

  private String publishSnapshotEventsAndCollectTopicsPartitionsOffsetsForSnapshots() {
    String topicsPartitionsOffsets = collectTopicsPartitionsOffsetsForSnapshots();
    iterateOverAllDomainEntities(this::publishDomainEntity);
    return topicsPartitionsOffsets;
  }

  private String collectTopicsPartitionsOffsetsForSnapshots() {
    return eventuateKafkaProducer
            .partitionsFor(domainClass.getName())
            .stream()
            .map(this::publishSnapshotOffsetEvent)
            .collect(Collectors.toList()) // avoids stream laziness to make sure that all events are published before waiting futures
            .stream()
            .map(this::getRecordMetadataFromFuture)
            .map(recordMetadata ->
                    String.format("%s:%s:%s",
                            recordMetadata.topic(),
                            recordMetadata.partition(),
                            recordMetadata.offset() + 1))
            .collect(Collectors.joining(" "));
  }

  void iterateOverAllDomainEntities(Consumer<T> callback) {
    for (int i = 0; ; i++) {
      Page<T> page = domainRepository.findAll(new PageRequest(i, iterationPageSize));
      page.forEach(callback);
      if (page.isLast()) {
        break;
      }
    }
  }

  private void publishDomainEntity(T domainEntity) {
    DomainEventWithEntityId domainEventWithEntityId = domainEntityToDomainEventConverter.apply(domainEntity);

    domainEventPublisher.publish(domainClass,
            domainEventWithEntityId.getEntityId(),
            Collections.singletonList(domainEventWithEntityId.getDomainEvent()));
  }

  private CompletableFuture<?> publishSnapshotOffsetEvent(PartitionInfo partitionInfo) {

    Message message = MessageBuilder
            .withPayload(JSonMapper.toJson(new SnapshotOffsetEvent()))
            .withHeader(Message.ID, idGenerator.genId().asString())
            .withHeader(EventMessageHeaders.AGGREGATE_TYPE, domainClass.getName())
            .withHeader(EventMessageHeaders.EVENT_TYPE, SnapshotOffsetEvent.class.getName())
            .build();

    return eventuateKafkaProducer.send(domainClass.getName(),
            null,
            JSonMapper.toJson(message));
  }

  private RecordMetadata getRecordMetadataFromFuture(CompletableFuture<?> future) {
    try {
      return (RecordMetadata) future.get();
    } catch (ExecutionException | InterruptedException e) {
      logger.error(e.getMessage());
      throw new RuntimeException(e);
    }
  }
}
