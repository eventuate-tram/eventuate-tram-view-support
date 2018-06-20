package io.eventuate.examples.tram.ordersandcustomers.snapshots;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.PostgreSQL82Dialect;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.function.Supplier;

public class DBLockServiceTest {

  @Test
  public void testPostgresQuery() {
    DBLockService dbLockService = Mockito.mock(DBLockService.class);

    Mockito.when(dbLockService.getDialect()).thenReturn(new PostgreSQL82Dialect());

    DBLockService.LockSpecification lockSpecification = new DBLockService.LockSpecification(new DBLockService.TableSpec("table1"),
            DBLockService.LockType.READ);

    Mockito.doCallRealMethod().when(dbLockService).createLockQueryDependingOnDatabase(lockSpecification);
    Mockito.doCallRealMethod().when(dbLockService).buildPostgresLockQuery(lockSpecification);

    Assert.assertEquals("lock table table1 in EXCLUSIVE mode;", dbLockService.createLockQueryDependingOnDatabase(lockSpecification));
  }

  @Test
  public void testMySqlQuery() {
    DBLockService dbLockService = Mockito.mock(DBLockService.class);

    Mockito.when(dbLockService.getDialect()).thenReturn(new MySQLDialect());

    DBLockService.LockSpecification lockSpecification = new DBLockService.LockSpecification(new DBLockService.TableSpec("table1"),
            DBLockService.LockType.READ);

    Mockito.doNothing().when(dbLockService).executeUnlockQueryIfNecessary();

    Mockito.doCallRealMethod().when(dbLockService).buildMySqlLockQuery(lockSpecification);
    Mockito.doCallRealMethod().when(dbLockService).createLockQueryDependingOnDatabase(lockSpecification);

    Assert.assertEquals("lock tables table1 read;", dbLockService.createLockQueryDependingOnDatabase(lockSpecification));
  }

  @Test(expected = DatabaseIsNotSupportedException.class)
  public void testNotSupportedDatabase() {
    DBLockService dbLockService = Mockito.mock(DBLockService.class);

    Mockito.when(dbLockService.getDialect()).thenReturn(new H2Dialect());

    DBLockService.LockSpecification lockSpecification = new DBLockService.LockSpecification(new DBLockService.TableSpec("table1"),
            DBLockService.LockType.READ);

    Mockito.doCallRealMethod().when(dbLockService).createLockQueryDependingOnDatabase(lockSpecification);

    dbLockService.createLockQueryDependingOnDatabase(lockSpecification);
  }

  @Test
  public void testPostgresCalls() {
    DBLockService.LockSpecification lockSpecification = new DBLockService.LockSpecification(new DBLockService.TableSpec("table1"),
            DBLockService.LockType.READ);

    DBLockService dbLockService = Mockito.mock(DBLockService.class);
    Supplier<Object> lockCallback = Mockito.mock(Supplier.class);
    JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);

    Mockito.doCallRealMethod().when(dbLockService).setJdbcTemplate(jdbcTemplate);
    dbLockService.setJdbcTemplate(jdbcTemplate);

    Mockito.when(dbLockService.getDialect()).thenReturn(new PostgreSQL82Dialect());

    Mockito.doCallRealMethod().when(dbLockService).withLockedTables(lockSpecification, lockCallback);
    Mockito.doCallRealMethod().when(dbLockService).createLockQueryDependingOnDatabase(lockSpecification);
    Mockito.doCallRealMethod().when(dbLockService).executeUnlockQueryIfNecessary();
    Mockito.doCallRealMethod().when(dbLockService).buildMySqlLockQuery(lockSpecification);
    Mockito.doCallRealMethod().when(dbLockService).buildPostgresLockQuery(lockSpecification);

    dbLockService.withLockedTables(lockSpecification, lockCallback);

    Mockito.verify(lockCallback).get();
    Mockito.verify(dbLockService).createLockQueryDependingOnDatabase(lockSpecification);
    Mockito.verify(dbLockService, Mockito.never()).buildMySqlLockQuery(lockSpecification);
    Mockito.verify(dbLockService).buildPostgresLockQuery(lockSpecification);
    Mockito.verify(dbLockService, Mockito.never()).unlockMysqlTables();

    Mockito.verify(jdbcTemplate).execute(dbLockService.buildPostgresLockQuery(lockSpecification));
  }

  @Test
  public void testMySqlCalls() {
    DBLockService.LockSpecification lockSpecification = new DBLockService.LockSpecification(new DBLockService.TableSpec("table1"),
            DBLockService.LockType.READ);

    DBLockService dbLockService = Mockito.mock(DBLockService.class);
    Supplier<Object> lockCallback = Mockito.mock(Supplier.class);
    JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);

    Mockito.doCallRealMethod().when(dbLockService).setJdbcTemplate(jdbcTemplate);
    dbLockService.setJdbcTemplate(jdbcTemplate);

    Mockito.when(dbLockService.getDialect()).thenReturn(new MySQLDialect());

    Mockito.doCallRealMethod().when(dbLockService).withLockedTables(lockSpecification, lockCallback);
    Mockito.doCallRealMethod().when(dbLockService).createLockQueryDependingOnDatabase(lockSpecification);
    Mockito.doCallRealMethod().when(dbLockService).executeUnlockQueryIfNecessary();
    Mockito.doCallRealMethod().when(dbLockService).buildMySqlLockQuery(lockSpecification);
    Mockito.doCallRealMethod().when(dbLockService).buildPostgresLockQuery(lockSpecification);

    dbLockService.withLockedTables(lockSpecification, lockCallback);

    Mockito.verify(lockCallback).get();
    Mockito.verify(dbLockService).createLockQueryDependingOnDatabase(lockSpecification);
    Mockito.verify(dbLockService).buildMySqlLockQuery(lockSpecification);
    Mockito.verify(dbLockService, Mockito.never()).buildPostgresLockQuery(lockSpecification);
    Mockito.verify(dbLockService).unlockMysqlTables();

    Mockito.verify(jdbcTemplate).execute(dbLockService.buildMySqlLockQuery(lockSpecification));
  }
}
