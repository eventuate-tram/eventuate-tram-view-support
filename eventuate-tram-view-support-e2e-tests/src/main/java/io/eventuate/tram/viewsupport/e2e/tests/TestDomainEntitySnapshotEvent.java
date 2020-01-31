package io.eventuate.tram.viewsupport.e2e.tests;

import io.eventuate.tram.events.common.DomainEvent;

public class TestDomainEntitySnapshotEvent implements DomainEvent {
  private Long id;
  private String data;

  public TestDomainEntitySnapshotEvent() {
  }

  public TestDomainEntitySnapshotEvent(Long id, String data) {
    this.id = id;
    this.data = data;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getData() {
    return data;
  }

  public void setData(String data) {
    this.data = data;
  }
}
