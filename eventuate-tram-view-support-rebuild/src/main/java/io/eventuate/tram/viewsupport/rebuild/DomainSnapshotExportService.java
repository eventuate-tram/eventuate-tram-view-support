package io.eventuate.tram.viewsupport.rebuild;

import com.google.common.collect.ImmutableMap;
import io.eventuate.javaclient.commonimpl.JSonMapper;
import io.eventuate.javaclient.spring.jdbc.IdGenerator;
import io.eventuate.local.java.kafka.producer.EventuateKafkaProducer;
import io.eventuate.tram.events.common.EventMessageHeaders;
import io.eventuate.tram.messaging.common.Message;
import io.eventuate.tram.messaging.producer.MessageBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.web.client.RestTemplate;

import java.util.function.Consumer;
import java.util.function.Function;

public class DomainSnapshotExportService<T> {
  protected PagingAndSortingRepository<T, Long> domainRepository;

  private EventuateKafkaProducer eventuateKafkaProducer;
  private DBLockService dbLockService;
  private IdGenerator idGenerator;

  private Class<T> domainClass;
  private Function<T, DomainEventWithEntityId> domainEntityToDomainEventConverter;
  private DBLockService.TableSpec domainTableSpec;
  private int iterationPageSize;
  private String cdcServiceUrl;
  private String cdcStatusServiceEndPoint;
  private int maxIterationsToCheckCdcProcessing;
  private int timeoutBetweenCdcProcessingCheckingIterationsInMilliseconds;
  private RestTemplate restTemplate = new RestTemplate();

  public DomainSnapshotExportService(EventuateKafkaProducer eventuateKafkaProducer,
                                     DBLockService dbLockService,
                                     IdGenerator idGenerator,
                                     Class<T> domainClass,
                                     PagingAndSortingRepository<T, Long> domainRepository,
                                     Function<T, DomainEventWithEntityId> domainEntityToDomainEventConverter,
                                     DBLockService.TableSpec domainTableSpec,
                                     int iterationPageSize,
                                     String cdcServiceUrl,
                                     String cdcStatusServiceEndPoint,
                                     int maxIterationsToCheckCdcProcessing,
                                     int timeoutBetweenCdcProcessingCheckingIterationsInMilliseconds) {

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
    this.maxIterationsToCheckCdcProcessing = maxIterationsToCheckCdcProcessing;
    this.timeoutBetweenCdcProcessingCheckingIterationsInMilliseconds = timeoutBetweenCdcProcessingCheckingIterationsInMilliseconds;
  }

  public void exportSnapshots() {
    DBLockService.LockSpecification lockSpecification = new DBLockService.LockSpecification(domainTableSpec,
            DBLockService.LockType.READ,
            ImmutableMap.of(new DBLockService.TableSpec("message"), DBLockService.LockType.WRITE));

    dbLockService.withLockedTables(lockSpecification, this::publishSnapshotEvents);
  }

  private Void publishSnapshotEvents() {
    for (int i = 1; i <= maxIterationsToCheckCdcProcessing; i++) {
      if (getCdcProcessingStatus().isLastEventOffsetEqualsToHighwaterMark()) {
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

    iterateOverAllDomainEntities(this::publishDomainEntity);
    return null;
  }

  private CdcProcessingStatus getCdcProcessingStatus() {
    return restTemplate.getForObject(String.format("%s/%s", cdcServiceUrl, cdcStatusServiceEndPoint), CdcProcessingStatus.class);
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

    Message message = MessageBuilder
            .withPayload(JSonMapper.toJson(domainEventWithEntityId.getDomainEvent()))
            .withHeader(Message.ID, idGenerator.genId().asString())
            .withHeader(EventMessageHeaders.AGGREGATE_ID, domainEventWithEntityId.getEntityId().toString())
            .withHeader(EventMessageHeaders.AGGREGATE_TYPE, domainClass.getName())
            .withHeader(EventMessageHeaders.EVENT_TYPE, domainEventWithEntityId.getDomainEvent().getClass().getName())
            .build();

    eventuateKafkaProducer.send(domainClass.getName(),
            domainEventWithEntityId.getEntityId().toString(),
            JSonMapper.toJson(message));
  }
}
