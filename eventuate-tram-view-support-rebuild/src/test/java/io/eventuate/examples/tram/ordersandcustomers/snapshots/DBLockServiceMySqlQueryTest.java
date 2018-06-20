package io.eventuate.examples.tram.ordersandcustomers.snapshots;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

public class DBLockServiceMySqlQueryTest {

  @Test
  public void testSingleTable() {
    DBLockService dbLockService = new DBLockService();
    DBLockService.TableSpec mainTableSpec = new DBLockService.TableSpec("TestTable1");
    DBLockService.LockSpecification lockSpecification = new DBLockService.LockSpecification(mainTableSpec, DBLockService.LockType.READ);
    String query = dbLockService.buildMySqlLockQuery(lockSpecification);

    Assert.assertEquals("lock tables TestTable1 read;", query);
  }

  @Test
  public void testSingleTableWithSynonym() {
    DBLockService dbLockService = new DBLockService();
    DBLockService.TableSpec mainTableSpec = new DBLockService.TableSpec("TestTable1", "t1");
    DBLockService.LockSpecification lockSpecification = new DBLockService.LockSpecification(mainTableSpec, DBLockService.LockType.WRITE);
    String query = dbLockService.buildMySqlLockQuery(lockSpecification);

    Assert.assertEquals("lock tables TestTable1 t1 write;", query);
  }

  @Test
  public void test2Tables() {
    DBLockService dbLockService = new DBLockService();
    DBLockService.TableSpec mainTableSpec = new DBLockService.TableSpec("TestTable1");
    DBLockService.TableSpec additionalTableSpec = new DBLockService.TableSpec("TestTable2");
    DBLockService.LockSpecification lockSpecification = new DBLockService.LockSpecification(mainTableSpec,
            DBLockService.LockType.READ,
            Collections.singletonMap(additionalTableSpec, DBLockService.LockType.READ));
    String query = dbLockService.buildMySqlLockQuery(lockSpecification);

    Assert.assertEquals("lock tables TestTable1 read, TestTable2 read;", query);
  }

  @Test
  public void testS2TablesWithSynonyms() {
    DBLockService dbLockService = new DBLockService();
    DBLockService.TableSpec mainTableSpec = new DBLockService.TableSpec("TestTable1", "t1");
    DBLockService.TableSpec additionalTableSpec = new DBLockService.TableSpec("TestTable2", "t2");
    DBLockService.LockSpecification lockSpecification = new DBLockService.LockSpecification(mainTableSpec,
            DBLockService.LockType.READ,
            Collections.singletonMap(additionalTableSpec, DBLockService.LockType.READ));
    String query = dbLockService.buildMySqlLockQuery(lockSpecification);

    Assert.assertEquals("lock tables TestTable1 t1 read, TestTable2 t2 read;", query);
  }
}
