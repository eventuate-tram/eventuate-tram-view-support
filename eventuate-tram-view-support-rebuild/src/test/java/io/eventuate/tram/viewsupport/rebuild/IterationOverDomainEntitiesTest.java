package io.eventuate.tram.viewsupport.rebuild;

import io.eventuate.common.spring.jdbc.EventuateCommonJdbcOperationsConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest(classes = IterationOverDomainEntitiesTest.Config.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class IterationOverDomainEntitiesTest {
  @Configuration
  @EnableJpaRepositories
  @EnableAutoConfiguration
  @Import(EventuateCommonJdbcOperationsConfiguration.class)
  public static class Config {
    @Bean
    public SnapshotterConfigurationProperties snapshotterConfigurationProperties() {
      return new SnapshotterConfigurationProperties();
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
    DomainSnapshotExportService domainSnapshotExportService = new DomainSnapshotExportService(
            null,
            null,
            null,
            null,
            null,
            TestEntity.class,
            testRepository,
            null,
            null,
            snapshotterConfigurationProperties.getDomainRepositoryPageSize(),
            snapshotterConfigurationProperties.getCdcServiceUrl(),
            snapshotterConfigurationProperties.getCdcStatusServiceEndPoint(),
            null,
            snapshotterConfigurationProperties.getMaxIterationsToCheckCdcProcessing(),
            snapshotterConfigurationProperties.getTimeoutBetweenCdcProcessingCheckingIterationsInMilliseconds());

    testRepository.deleteAll();

    for (int i = 0; i < elements; i++) {
      testRepository.save(new TestEntity());
    }

    AtomicInteger counter = new AtomicInteger(0);
    domainSnapshotExportService.iterateOverAllDomainEntities(testEntity -> counter.incrementAndGet());

    Assertions.assertEquals(elements, counter.get());
    testRepository.deleteAll();
  }
}
