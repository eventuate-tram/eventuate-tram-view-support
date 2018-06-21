package io.eventuate.tram.viewsupport.rebuild;

import io.eventuate.tram.events.common.DomainEvent;

public class DomainEventWithEntityId {
  private Object entityId;
  private DomainEvent domainEvent;

  public DomainEventWithEntityId(Object entityId, DomainEvent domainEvent) {
    this.entityId = entityId;
    this.domainEvent = domainEvent;
  }

  public Object getEntityId() {
    return entityId;
  }

  public DomainEvent getDomainEvent() {
    return domainEvent;
  }
}
