package io.eventuate.tram.viewsupport.rebuild;

import io.eventuate.common.spring.jdbc.EventuateCommonJdbcOperationsConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.concurrent.*;
import java.util.function.Supplier;

@SpringBootTest(classes = TableLockingTest.Config.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class TableLockingTest {

  @Configuration
  @EnableJpaRepositories
  @EnableAutoConfiguration
  @Import(EventuateCommonJdbcOperationsConfiguration.class)
  public static class Config {
    @Bean
    public DBDialectDeterminer dbDialectDeterminer() {
      return new DBDialectDeterminer();
    }

    @Bean
    public DBLockService dbLockService(JdbcTemplate jdbcTemplate, DBDialectDeterminer dbDialectDeterminer) {
      return new DBLockService(jdbcTemplate, dbDialectDeterminer);
    }
  }

  @Autowired
  private DBLockService dbLockService;

  @Autowired
  private TestRepository testRepository;

  @Test
  public void checkThatQueryHangsWhenTableIsLocked() {
    Assertions.assertThrows(TimeoutException.class,
            () -> testLock(10000, () -> testRepository.save(new TestEntity())));
  }

  @Test
  public void checkThatQueryWorksWhenTableLockIsReleased() throws TimeoutException {
    testLock(1, () -> testRepository.save(new TestEntity()));
  }

  @Test
  public void checkThatSelectQueryWorks() throws TimeoutException {
    testLock(10000, () -> testRepository.findAll());
  }

  private void testLock(long timeInMilliseconds, Supplier<?> query) throws TimeoutException {
    CountDownLatch countDownLatch = new CountDownLatch(1);

    DBLockService.LockSpecification lockSpecification = new DBLockService.LockSpecification(new DBLockService.TableSpec("testentity", "testentity0_"),
            DBLockService.LockType.READ);

    CompletableFuture.supplyAsync(() -> {
      dbLockService.withLockedTables(lockSpecification, () -> {
        try {
          countDownLatch.countDown();
          Thread.sleep(timeInMilliseconds);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
        return null;
      });

      return null;
    });

    try {
      countDownLatch.await(30, TimeUnit.SECONDS);

      CompletableFuture
              .supplyAsync(query)
              .get(2, TimeUnit.SECONDS);
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }
}

