package io.eventuate.tram.viewsupport.rebuild;

import org.springframework.beans.factory.annotation.Value;

public class SnapshotterConfigurationProperties {
  @Value("${domain.repository.page.size:#{20}}")
  private int domainRepositoryPageSize;

  @Value("${cdc.service.url}")
  private String cdcServiceUrl;

  @Value("${cdc.status.service.end.point:#{\"cdc-event-processing-status\"}}")
  private String cdcStatusServiceEndPoint;

  @Value("${max.iterations.to.check.cdc.processing:#{30}}")
  private int maxIterationsToCheckCdcProcessing;

  @Value("${timeout.between.cdc.processing.checking.iterations.in.milliseconds:#{1000}}")
  private int timeoutBetweenCdcProcessingCheckingIterationsInMilliseconds;

  public int getDomainRepositoryPageSize() {
    return domainRepositoryPageSize;
  }

  public String getCdcServiceUrl() {
    return cdcServiceUrl;
  }

  public String getCdcStatusServiceEndPoint() {
    return cdcStatusServiceEndPoint;
  }

  public int getMaxIterationsToCheckCdcProcessing() {
    return maxIterationsToCheckCdcProcessing;
  }

  public int getTimeoutBetweenCdcProcessingCheckingIterationsInMilliseconds() {
    return timeoutBetweenCdcProcessingCheckingIterationsInMilliseconds;
  }
}
