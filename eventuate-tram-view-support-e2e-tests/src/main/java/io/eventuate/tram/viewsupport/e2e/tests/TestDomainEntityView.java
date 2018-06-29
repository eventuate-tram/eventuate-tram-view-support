package io.eventuate.tram.viewsupport.e2e.tests;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class TestDomainEntityView {
  @Id
  private Long id;

  private String data;

  public TestDomainEntityView() {
  }

  public TestDomainEntityView(String data) {
    this.data = data;
  }

  public Long getId() {
    return id;
  }

  public String getData() {
    return data;
  }

  public void setData(String data) {
    this.data = data;
  }
}
