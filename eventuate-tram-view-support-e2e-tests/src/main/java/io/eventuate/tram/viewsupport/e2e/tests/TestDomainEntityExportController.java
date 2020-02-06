package io.eventuate.tram.viewsupport.e2e.tests;

import io.eventuate.tram.viewsupport.rebuild.DomainSnapshotExportService;
import io.eventuate.tram.viewsupport.rebuild.PartitionOffset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TestDomainEntityExportController {

  @Autowired
  private DomainSnapshotExportService<TestDomainEntity> snapshotExportService;

  @RequestMapping(value = "/export/test-domain-entity", method = RequestMethod.POST)
  public List<PartitionOffset> exportSnapshots() {
    return snapshotExportService.exportSnapshots();
  }
}
