package io.eventuate.tram.viewsupport.e2e.tests;

import io.eventuate.messaging.kafka.basic.consumer.EventuateKafkaConsumerConfigurationProperties;
import io.eventuate.tram.consumer.kafka.EventuateTramKafkaMessageConsumerConfiguration;
import io.eventuate.tram.events.common.DomainEvent;
import io.eventuate.tram.events.common.DomainEventNameMapping;
import io.eventuate.tram.events.subscriber.DomainEventDispatcher;
import io.eventuate.tram.messaging.consumer.MessageConsumer;
import io.eventuate.tram.viewsupport.rebuild.*;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
@Import({SnapshotConfiguration.class, EventuateTramKafkaMessageConsumerConfiguration.class})
@EnableConfigurationProperties(EventuateKafkaConsumerConfigurationProperties.class)
public class EventuateTramViewSupportE2ETestConfiguration {
  @Bean
  public TestDomainEntityViewEventConsumer testDomainEntityViewEventConsumer() {
    return new TestDomainEntityViewEventConsumer();
  }

  @Bean("testDomainEntityDomainEventDispatcher")
  public DomainEventDispatcher testDomainEntityDomainEventDispatcher(TestDomainEntityViewEventConsumer testDomainEntityViewEventConsumer,
                                                                     MessageConsumer messageConsumer,
                                                                     DomainEventNameMapping domainEventNameMapping) {

    return new DomainEventDispatcher("testDomainEntityServiceEvents",
            testDomainEntityViewEventConsumer.domainEventHandlers(), messageConsumer, domainEventNameMapping);
  }

  @Bean
  public DomainSnapshotExportService<TestDomainEntity> domainSnapshotExportService(TestDomainEntityRepository testDomainEntityRepository,
                                                                                   DomainSnapshotExportServiceFactory<TestDomainEntity> domainSnapshotExportServiceFactory) {
    return domainSnapshotExportServiceFactory.make(
            TestDomainEntity.class,
            testDomainEntityRepository,
            testDomainEntity -> {
              DomainEvent domainEvent = new TestDomainEntitySnapshotEvent(testDomainEntity.getId(), testDomainEntity.getData());
              return new DomainEventWithEntityId(testDomainEntity.getId(), domainEvent);
            },
            new DBLockService.TableSpec("testdomainentity", "testdomain0_"),
            "Reader");
  }
}