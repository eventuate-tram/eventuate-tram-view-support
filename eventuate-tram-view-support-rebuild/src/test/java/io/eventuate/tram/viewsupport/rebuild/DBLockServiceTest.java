package io.eventuate.tram.viewsupport.rebuild;

import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.function.Supplier;

public class DBLockServiceTest {

  @Test
  public void testPostgresQuery() {
    JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
    DBDialectDeterminer dbDialectDeterminer = Mockito.mock(DBDialectDeterminer.class);
    Mockito.when(dbDialectDeterminer.getDialect()).thenReturn(new PostgreSQLDialect());
    DBLockService dbLockService = new DBLockService(jdbcTemplate, dbDialectDeterminer);

    DBLockService.LockSpecification lockSpecification = new DBLockService.LockSpecification(new DBLockService.TableSpec("table1"),
            DBLockService.LockType.READ);

    Supplier<Object> callback = Mockito.mock(Supplier.class);
    dbLockService.withLockedTables(lockSpecification, callback);

    Mockito.verify(jdbcTemplate).execute("lock table table1 in EXCLUSIVE mode;");
    Mockito.verify(callback).get();
    Mockito.verify(jdbcTemplate, Mockito.times(1)).execute(Mockito.anyString());
  }

  @Test
  public void testMysqlQuery() {
    JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
    DBDialectDeterminer dbDialectDeterminer = Mockito.mock(DBDialectDeterminer.class);
    Mockito.when(dbDialectDeterminer.getDialect()).thenReturn(new MySQLDialect());
    DBLockService dbLockService = new DBLockService(jdbcTemplate, dbDialectDeterminer);

    DBLockService.LockSpecification lockSpecification = new DBLockService.LockSpecification(new DBLockService.TableSpec("table1"),
            DBLockService.LockType.READ);

    Supplier<Object> callback = Mockito.mock(Supplier.class);
    dbLockService.withLockedTables(lockSpecification, callback);

    Mockito.verify(jdbcTemplate).execute("lock tables table1 read;");
    Mockito.verify(callback).get();
    Mockito.verify(jdbcTemplate).execute("unlock tables;");
    Mockito.verify(jdbcTemplate, Mockito.times(2)).execute(Mockito.anyString());
  }
}
