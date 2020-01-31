package io.eventuate.tram.viewsupport.rebuild;

public class CdcProcessingStatus {
  private long lastEventOffset;
  private long logsHighwaterMark;
  private boolean cdcProcessingFinished;

  public CdcProcessingStatus() {
  }

  public CdcProcessingStatus(long lastEventOffset, long logsHighwaterMark, boolean cdcProcessingFinished) {
    this.lastEventOffset = lastEventOffset;
    this.logsHighwaterMark = logsHighwaterMark;
    this.cdcProcessingFinished = cdcProcessingFinished;
  }

  public long getLastEventOffset() {
    return lastEventOffset;
  }

  public void setLastEventOffset(long lastEventOffset) {
    this.lastEventOffset = lastEventOffset;
  }

  public long getLogsHighwaterMark() {
    return logsHighwaterMark;
  }

  public void setLogsHighwaterMark(long logsHighwaterMark) {
    this.logsHighwaterMark = logsHighwaterMark;
  }

  public boolean isCdcProcessingFinished() {
    return cdcProcessingFinished;
  }

  public void setCdcProcessingFinished(boolean cdcProcessingFinished) {
    this.cdcProcessingFinished = cdcProcessingFinished;
  }
}
