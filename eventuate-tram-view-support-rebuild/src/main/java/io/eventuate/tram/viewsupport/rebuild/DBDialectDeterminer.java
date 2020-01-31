package io.eventuate.tram.viewsupport.rebuild;

import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.hibernate.internal.SessionFactoryImpl;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.EntityManager;

public class DBDialectDeterminer {
  @Autowired
  private EntityManager entityManager;

  Dialect getDialect() {
    Session session = (Session) entityManager.getDelegate();
    return ((SessionFactoryImpl)session.getSessionFactory()).getDialect();
  }
}
