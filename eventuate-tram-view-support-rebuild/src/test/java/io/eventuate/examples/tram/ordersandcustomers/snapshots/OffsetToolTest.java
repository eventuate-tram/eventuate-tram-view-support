package io.eventuate.examples.tram.ordersandcustomers.snapshots;

import org.apache.commons.lang.StringUtils;
import org.aspectj.util.FileUtil;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

public class OffsetToolTest {

  @Test
  public void testThatOffsetAssignedCorrectly() throws Exception {
    String topic = UUID.randomUUID().toString();
    String subscriber = UUID.randomUUID().toString();
    String messages = "m1\rm2\rm3\r";

    new ProcessBuilder("../create-kafka-topic.sh", topic).start().waitFor();
    new ProcessBuilder("../send-kafka-messages.sh", topic, messages).start().waitFor();

    Process setOffsetsProcess = new ProcessBuilder("../set-kafka-offsets.sh", subscriber, topic + ":0:3").start();
    setOffsetsProcess.waitFor();

    String result = new String(FileUtil.readAsByteArray(setOffsetsProcess.getInputStream()));

    String offset = Arrays
            .asList(result.substring(result.indexOf(topic)).split(" "))
            .stream()
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toList())
            .get(2);

    Assert.assertEquals("3", offset);
  }
}
