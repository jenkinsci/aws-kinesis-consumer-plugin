package io.jenkins.plugins.aws.kinesisconsumer.extensions;

import static org.junit.Assert.assertEquals;

import hudson.Extension;
import hudson.ExtensionList;
import io.jenkins.plugins.aws.kinesisconsumer.BaseLocalStack;
import io.jenkins.plugins.aws.kinesisconsumer.utils.WaitUtil;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;

public class AWSKinesisStreamListenerTest extends BaseLocalStack {

  @Extension
  public static class TestListener extends AWSKinesisStreamListener {
    private Map<String, Integer> recordReceivedPerStream;

    public TestListener() {
      recordReceivedPerStream = new HashMap<>();
    }

    Integer getRecordReceivedCounter(String streamName) {
      return recordReceivedPerStream.getOrDefault(streamName, 0);
    }

    @Override
    public void onReceive(String streamName, byte[] byteRecord) {
      recordReceivedPerStream.put(streamName, getRecordReceivedCounter(streamName) + 1);
    }
  }

  @Test
  public void shouldConsumeAnEventPublishedToAStream() throws Exception {
    createStreamAndWait(STREAM_NAME);
    kinesisConsumerManager.startAllConsumers(globalKinesisConfiguration);

    waitForLeaseTable();

    ExtensionList<AWSKinesisStreamListener> extensionList =
        AWSKinesisStreamListener.getAllRegisteredListeners();
    assertEquals(1, extensionList.size());

    TestListener testListener = (TestListener) extensionList.get(0);

    sendKinesisRecord();

    WaitUtil.waitUntil(
        () -> testListener.getRecordReceivedCounter(STREAM_NAME) == 1, RECORD_CONSUMED_TIMEOUT);
  }

  private void sendKinesisRecord() {
    PutRecordRequest putRecordRequest =
        PutRecordRequest.builder()
            .streamName(STREAM_NAME)
            .partitionKey("testPartitionKey")
            .data(SdkBytes.fromUtf8String("someData"))
            .build();
    kinesisClient.putRecord(putRecordRequest);
  }
}
