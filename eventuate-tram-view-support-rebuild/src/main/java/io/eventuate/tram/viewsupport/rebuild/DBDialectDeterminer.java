package io.eventuate.tram.viewsupport.rebuild;

import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.persistence.EntityManager;

public class DBDialectDeterminer {
  @Autowired
  private EntityManager entityManager;

  Dialect getDialect() {
    Session session = (Session) entityManager.getDelegate();
    return session.getSessionFactory().unwrap(SessionFactoryImplementor.class).getJdbcServices().getDialect();
  }
}
