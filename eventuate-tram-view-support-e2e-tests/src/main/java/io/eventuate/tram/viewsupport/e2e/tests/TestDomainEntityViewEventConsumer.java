package io.eventuate.tram.viewsupport.e2e.tests;

import io.eventuate.tram.events.subscriber.DomainEventEnvelope;
import io.eventuate.tram.events.subscriber.DomainEventHandlers;
import io.eventuate.tram.events.subscriber.DomainEventHandlersBuilder;
import org.springframework.beans.factory.annotation.Autowired;


public class TestDomainEntityViewEventConsumer {
  @Autowired
  private TestDomainEntityViewRepository testDomainEntityViewRepository;

  public DomainEventHandlers domainEventHandlers() {
    return DomainEventHandlersBuilder
            .forAggregateType(TestDomainEntity.class.getName())
            .onEvent(TestDomainEntitySnapshotEvent.class, this::testDomainEntitySnapshotEventHandler)
            .build();
  }

  void testDomainEntitySnapshotEventHandler(DomainEventEnvelope<TestDomainEntitySnapshotEvent> domainEventEnvelope) {
    TestDomainEntitySnapshotEvent testDomainEntitySnapshotEvent = domainEventEnvelope.getEvent();
    testDomainEntityViewRepository.addTestDomainEntityView(testDomainEntitySnapshotEvent.getId(), testDomainEntitySnapshotEvent.getData());
  }
}
