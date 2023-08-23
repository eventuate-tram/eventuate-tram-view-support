package io.eventuate.tram.viewsupport.e2e.tests;

import io.eventuate.messaging.kafka.spring.basic.consumer.EventuateKafkaConsumerSpringConfigurationPropertiesConfiguration;
import io.eventuate.tram.events.subscriber.DomainEventDispatcherFactory;
import io.eventuate.tram.spring.consumer.kafka.EventuateTramKafkaMessageConsumerConfiguration;
import io.eventuate.tram.events.common.DomainEvent;
import io.eventuate.tram.events.common.DomainEventNameMapping;
import io.eventuate.tram.events.subscriber.DomainEventDispatcher;
import io.eventuate.tram.messaging.consumer.MessageConsumer;
import io.eventuate.tram.spring.events.subscriber.TramEventSubscriberConfiguration;
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
@Import({SnapshotConfiguration.class,
        EventuateTramKafkaMessageConsumerConfiguration.class,
        EventuateKafkaConsumerSpringConfigurationPropertiesConfiguration.class,
        TramEventSubscriberConfiguration.class})
public class EventuateTramViewSupportE2ETestConfiguration {
  @Bean
  public TestDomainEntityViewEventConsumer testDomainEntityViewEventConsumer() {
    return new TestDomainEntityViewEventConsumer();
  }

  @Bean("testDomainEntityDomainEventDispatcher")
  public DomainEventDispatcher testDomainEntityDomainEventDispatcher(DomainEventDispatcherFactory domainEventDispatcherFactory,
                                                                     TestDomainEntityViewEventConsumer testDomainEntityViewEventConsumer) {

    return domainEventDispatcherFactory.make("testDomainEntityServiceEvents",
            testDomainEntityViewEventConsumer.domainEventHandlers());
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