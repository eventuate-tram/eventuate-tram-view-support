package io.eventuate.viewsupport.rebuild;

import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.EntityManager;
import java.lang.reflect.InvocationTargetException;

public class DBDialectDeterminer {
  @Autowired
  private EntityManager entityManager;

  Dialect getDialect() {
    Session session = (Session) entityManager.getDelegate();
    try {
      return (Dialect)PropertyUtils.getProperty(session.getSessionFactory(), "dialect");
    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }
}
