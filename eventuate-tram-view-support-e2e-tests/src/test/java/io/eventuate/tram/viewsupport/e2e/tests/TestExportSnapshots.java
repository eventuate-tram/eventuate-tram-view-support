package io.eventuate.tram.viewsupport.e2e.tests;

import io.eventuate.tram.viewsupport.rebuild.DomainSnapshotExportService;
import io.eventuate.tram.viewsupport.rebuild.SnapshotMetadata;
import io.eventuate.util.test.async.Eventually;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = EventuateTramViewSupportE2ETestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TestExportSnapshots {

  @Autowired
  private TestDomainEntityRepository testDomainEntityRepository;

  @Autowired
  private TestDomainEntityViewRepository testDomainEntityViewRepository;

  @Autowired
  private DomainSnapshotExportService<TestDomainEntity> domainEntityDomainSnapshotExportService;

  @LocalServerPort
  private int port;

  private RestTemplate restTemplate = new RestTemplate();

  @Test
  public void testThatViewsMatchOriginalEntitiesAfterExport() {
    String dataPrefix = UUID.randomUUID().toString();
    int domainEntitiesCount = 10;

    Set<String> originalData = IntStream
            .range(0, domainEntitiesCount)
            .mapToObj(i -> {
              String data = dataPrefix + i;
              testDomainEntityRepository.save(new TestDomainEntity(data));
              return data;
            })
            .collect(Collectors.toSet());

    domainEntityDomainSnapshotExportService.exportSnapshots();

    SnapshotMetadata[] snapshotMetadata = restTemplate.postForObject(String.format("http://localhost:%s/export/test-domain-entity", port), null, SnapshotMetadata[].class);

    for (SnapshotMetadata meta : snapshotMetadata) {
      Assert.assertTrue(meta.getOffset() > 0);
      Assert.assertEquals(TestDomainEntity.class.getName(), meta.getTopic());
    }

    Eventually.eventually(() -> {
      List<TestDomainEntityView> views = testDomainEntityViewRepository.findAll();
      Set<String> viewData = views.stream().map(TestDomainEntityView::getData).collect(Collectors.toSet());
      Assert.assertEquals(originalData, viewData);
    });
  }

}
