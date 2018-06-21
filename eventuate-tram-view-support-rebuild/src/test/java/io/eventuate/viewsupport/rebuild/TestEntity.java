package io.eventuate.viewsupport.rebuild;

import javax.persistence.*;

@Entity
@Table(name="testentity")
@Access(AccessType.FIELD)
public class TestEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
}
