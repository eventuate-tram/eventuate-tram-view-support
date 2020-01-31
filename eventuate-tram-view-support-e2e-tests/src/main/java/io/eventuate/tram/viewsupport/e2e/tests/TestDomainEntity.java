package io.eventuate.tram.viewsupport.e2e.tests;

import javax.persistence.*;

@Entity
@Table(name="testdomainentity")
@Access(AccessType.FIELD)
public class TestDomainEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String data;

  public TestDomainEntity() {
  }

  public TestDomainEntity(String data) {
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
