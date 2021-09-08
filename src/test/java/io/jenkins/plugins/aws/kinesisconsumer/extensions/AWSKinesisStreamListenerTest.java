package io.jenkins.plugins.aws.kinesisconsumer.extensions;

import static org.junit.Assert.assertEquals;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.KINESIS;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import hudson.Extension;
import hudson.ExtensionList;
import io.jenkins.plugins.aws.kinesisconsumer.BaseLocalStack;
import io.jenkins.plugins.aws.kinesisconsumer.GlobalKinesisConfiguration;
import io.jenkins.plugins.aws.kinesisconsumer.KinesisConsumer;
import io.jenkins.plugins.aws.kinesisconsumer.KinesisConsumerModule;
import io.jenkins.plugins.aws.kinesisconsumer.KinesisStreamItem;
import io.jenkins.plugins.aws.kinesisconsumer.utils.WaitUtil;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;

public class AWSKinesisStreamListenerTest extends BaseLocalStack {
  public static final String LEASE_TABLE_NAME = "jenkins-kinesis-consumer-test-stream";
  private static final Duration TABLE_CREATION_TIMEOUT = Duration.ofSeconds(60);
  private static final Duration RECORD_CONSUMED_TIMEOUT = Duration.ofMinutes(5);

  public static final String STREAM_NAME = "test-stream";

  private static Injector injector;
  private GlobalKinesisConfiguration globalKinesisConfiguration;
  private KinesisConsumer.Factory kinesisConsumerFactory;
  private DynamoDbAsyncClient dynamoDbAsynClient;

  class TestKinesisConsumerModule extends AbstractModule {

    @Override
    protected void configure() {
      globalKinesisConfiguration = initGlobalConfig();
      bind(GlobalKinesisConfiguration.class).toInstance(globalKinesisConfiguration);
      install(new KinesisConsumerModule());
    }
  }

  @Before
  public void setUp() throws Exception {
    super.setUp();
    globalKinesisConfiguration = initGlobalConfig();

    System.setProperty("endpoint", localstack.getEndpointOverride(KINESIS).toASCIIString());
    System.setProperty("region", localstack.getRegion());
    System.setProperty("aws.accessKeyId", localstack.getAccessKey());
    System.setProperty("aws.secretAccessKey", localstack.getSecretKey());

    injector = Guice.createInjector(new TestKinesisConsumerModule());
    kinesisConsumerFactory = injector.getInstance(KinesisConsumer.Factory.class);
    dynamoDbAsynClient = injector.getInstance(DynamoDbAsyncClient.class);
  }

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
    KinesisConsumer kinesisConsumer = kinesisConsumerFactory.create(STREAM_NAME);
    kinesisConsumer.subscribe();

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

  private GlobalKinesisConfiguration initGlobalConfig() {
    GlobalKinesisConfiguration globalKinesisConfiguration = new GlobalKinesisConfiguration();
    globalKinesisConfiguration.setKinesisConsumerEnabled(true);
    globalKinesisConfiguration.setLocalEndpoint(
        localstack.getEndpointOverride(KINESIS).toASCIIString());
    globalKinesisConfiguration.setRegion(localstack.getRegion());
    globalKinesisConfiguration.setApplicationName("jenkins-kinesis-consumer");
    KinesisStreamItem kinesisStreamItem = new KinesisStreamItem(STREAM_NAME, "TRIM_HORIZON");

    globalKinesisConfiguration.setKinesisStreamItems(Collections.singletonList(kinesisStreamItem));

    globalKinesisConfiguration.load();
    return globalKinesisConfiguration;
  }

  private void waitForLeaseTable() throws InterruptedException {

    WaitUtil.waitUntil(
        () -> {
          try {
            ScanResponse sr =
                dynamoDbAsynClient
                    .scan(ScanRequest.builder().tableName(LEASE_TABLE_NAME).build())
                    .get();
            return !sr.items().isEmpty();
          } catch (ExecutionException | InterruptedException ignored) {
          }
          return false;
        },
        TABLE_CREATION_TIMEOUT);
  }
}
