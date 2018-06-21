package io.eventuate.tram.viewsupport.rebuild;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.concurrent.atomic.AtomicInteger;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = IterationOverDomainEntitiesTest.Config.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class IterationOverDomainEntitiesTest {
  @Configuration
  @EnableJpaRepositories
  @EnableAutoConfiguration
  @ComponentScan
  public static class Config {
    @Bean
    public DBDialectDeterminer dbDialectDeterminer() {
      return new DBDialectDeterminer();
    }
  }

  @Autowired
  private SnapshotterConfigurationProperties snapshotterConfigurationProperties;

  @Autowired
  private TestRepository testRepository;

  @Test
  public void testSeveralElements() {
    testIteration(3);
  }

  @Test
  public void testFullPage() {
    testIteration(snapshotterConfigurationProperties.getDomainRepositoryPageSize());
  }

  @Test
  public void testSeveralElementsNotFullPages() {
    testIteration(snapshotterConfigurationProperties.getDomainRepositoryPageSize() * 3 - 3);
  }

  @Test
  public void testSeveralElementsFullPages() {
    testIteration(snapshotterConfigurationProperties.getDomainRepositoryPageSize() * 3);
  }

  public void testIteration(int elements) {
    DomainSnapshotExportService domainSnapshotExportService = new DomainSnapshotExportService(null,
            null,
            null,
            null,
            TestEntity.class,
            testRepository, null, null, snapshotterConfigurationProperties.getDomainRepositoryPageSize());

    testRepository.deleteAll();

    for (int i = 0; i < elements; i++) {
      testRepository.save(new TestEntity());
    }

    AtomicInteger counter = new AtomicInteger(0);
    domainSnapshotExportService.iterateOverAllDomainEntities(testEntity -> counter.incrementAndGet());

    Assert.assertEquals(elements, counter.get());
    testRepository.deleteAll();
  }
}
