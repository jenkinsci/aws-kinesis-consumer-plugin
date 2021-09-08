package io.jenkins.plugins.aws.kinesisconsumer.extensions;

import static org.junit.Assert.assertEquals;

import hudson.Extension;
import hudson.ExtensionList;
import io.jenkins.plugins.aws.kinesisconsumer.BaseLocalStack;
import io.jenkins.plugins.aws.kinesisconsumer.utils.WaitUtil;
import org.junit.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;

public class AWSKinesisStreamListenerTest extends BaseLocalStack {

  @Extension
  public static class TestListener extends AWSKinesisStreamListener {
    private int recordReceivedCounter;

    public TestListener() {
      recordReceivedCounter = 0;
    }

    int getRecordReceivedCounter() {
      return recordReceivedCounter;
    }

    @Override
    public String getStreamName() {
      return STREAM_NAME;
    }

    @Override
    public void onReceive(byte[] byteRecord) {
      recordReceivedCounter++;
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

    WaitUtil.waitUntil(() -> testListener.getRecordReceivedCounter() == 1, RECORD_CONSUMED_TIMEOUT);
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
