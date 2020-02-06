package io.eventuate.tram.viewsupport.rebuild;

public class PartitionOffset {
  private int partition;
  private long offset;

  public PartitionOffset() {
  }

  public PartitionOffset(int partition, long offset) {
    this.partition = partition;
    this.offset = offset;
  }

  public int getPartition() {
    return partition;
  }

  public void setPartition(int partition) {
    this.partition = partition;
  }

  public long getOffset() {
    return offset;
  }

  public void setOffset(long offset) {
    this.offset = offset;
  }
}
