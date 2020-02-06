package io.eventuate.tram.viewsupport.e2e.tests;

import io.eventuate.common.id.IdGenerator;
import io.eventuate.messaging.kafka.producer.EventuateKafkaProducer;
import io.eventuate.tram.consumer.kafka.EventuateTramKafkaMessageConsumerConfiguration;
import io.eventuate.tram.events.common.DomainEvent;
import io.eventuate.tram.events.common.DomainEventNameMapping;
import io.eventuate.tram.events.publisher.TramEventsPublisherConfiguration;
import io.eventuate.tram.events.subscriber.DomainEventDispatcher;
import io.eventuate.tram.messaging.consumer.MessageConsumer;
import io.eventuate.tram.messaging.producer.jdbc.TramMessageProducerJdbcConfiguration;
import io.eventuate.tram.viewsupport.rebuild.*;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableAutoConfiguration
@ComponentScan
@EnableJpaRepositories
@EnableMongoRepositories
@Import({EventuateTramKafkaMessageConsumerConfiguration.class,
        TramEventsPublisherConfiguration.class,
        TramMessageProducerJdbcConfiguration.class,
        SnapshotConfiguration.class})
public class EventuateTramViewSupportE2ETestConfiguration {
  @Bean
  public TestDomainEntityViewEventConsumer testDomainEntityViewEventConsumer() {
    return new TestDomainEntityViewEventConsumer();
  }

  @Bean("testDomainEntityDomainEventDispatcher")
  public DomainEventDispatcher orderHistoryDomainEventDispatcher(TestDomainEntityViewEventConsumer testDomainEntityViewEventConsumer,
                                                                 MessageConsumer messageConsumer,
                                                                 DomainEventNameMapping domainEventNameMapping) {

    return new DomainEventDispatcher("testDomainEntityServiceEvents",
            testDomainEntityViewEventConsumer.domainEventHandlers(), messageConsumer, domainEventNameMapping);
  }

  @Bean
  public DomainSnapshotExportService<TestDomainEntity> domainSnapshotExportService(EventuateKafkaProducer eventuateKafkaProducer,
                                                                                   DBLockService dbLockService,
                                                                                   IdGenerator idGenerator,
                                                                                   TestDomainEntityRepository testDomainEntityRepository,
                                                                                   SnapshotterConfigurationProperties snapshotterConfigurationProperties) {
    return new DomainSnapshotExportService<>(eventuateKafkaProducer,
            dbLockService,
            idGenerator,
            TestDomainEntity.class,
            testDomainEntityRepository,
            testDomainEntity -> {
              DomainEvent domainEvent = new TestDomainEntitySnapshotEvent(testDomainEntity.getId(), testDomainEntity.getData());
              return new DomainEventWithEntityId(testDomainEntity.getId(), domainEvent);
            },
            new DBLockService.TableSpec("testdomainentity", "testdomain0_"),
            snapshotterConfigurationProperties.getDomainRepositoryPageSize(),
            snapshotterConfigurationProperties.getCdcServiceUrl(),
            snapshotterConfigurationProperties.getCdcStatusServiceEndPoint(),
            "Reader",
            snapshotterConfigurationProperties.getMaxIterationsToCheckCdcProcessing(),
            snapshotterConfigurationProperties.getTimeoutBetweenCdcProcessingCheckingIterationsInMilliseconds(),
            snapshotterConfigurationProperties.getKafkaPartitions());
  }
}