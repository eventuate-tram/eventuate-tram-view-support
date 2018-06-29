package io.eventuate.tram.viewsupport.rebuild;

public class CdcProcessingStatus {
  private long lastEventOffset;
  private long logsHighwaterMark;
  private boolean lastEventOffsetEqualsToHighwaterMark;

  public CdcProcessingStatus() {
  }

  public CdcProcessingStatus(long lastEventOffset, long logsHighwaterMark, boolean lastEventOffsetEqualsToHighwaterMark) {
    this.lastEventOffset = lastEventOffset;
    this.logsHighwaterMark = logsHighwaterMark;
    this.lastEventOffsetEqualsToHighwaterMark = lastEventOffsetEqualsToHighwaterMark;
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

  public boolean isLastEventOffsetEqualsToHighwaterMark() {
    return lastEventOffsetEqualsToHighwaterMark;
  }

  public void setLastEventOffsetEqualsToHighwaterMark(boolean lastEventOffsetEqualsToHighwaterMark) {
    this.lastEventOffsetEqualsToHighwaterMark = lastEventOffsetEqualsToHighwaterMark;
  }
}
