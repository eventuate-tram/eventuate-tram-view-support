package io.eventuate.examples.tram.ordersandcustomers.snapshots;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SnapshotterConfigurationProperties {
  @Value("${domain.repository.page.size:#{20}}")
  private int domainRepositoryPageSize;

  public int getDomainRepositoryPageSize() {
    return domainRepositoryPageSize;
  }
}
