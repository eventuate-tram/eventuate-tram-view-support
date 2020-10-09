package io.eventuate.tram.viewsupport.rebuild;

public class TopicPartitionOffsetMessageId {
  private String topic;
  private int partition;
  private long offset;
  private String messageId;

  public TopicPartitionOffsetMessageId() {
  }

  public TopicPartitionOffsetMessageId(String topic, int partition, long offset, String messageId) {
    this.topic = topic;
    this.partition = partition;
    this.offset = offset;
    this.messageId = messageId;
  }

  public String getTopic() {
    return topic;
  }

  public void setTopic(String topic) {
    this.topic = topic;
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

  public String getDatabaseId() {
    return messageId;
  }

  public void setDatabaseId(String databaseId) {
    this.messageId = databaseId;
  }
}
