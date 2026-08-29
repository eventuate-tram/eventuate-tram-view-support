package io.eventuate.tram.viewsupport.rebuild;

import jakarta.persistence.*;

@Entity
@Table(name="testentity")
@Access(AccessType.FIELD)
public class TestEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
}
