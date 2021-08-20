package io.jenkins.plugins.aws.kinesisconsumer.extensions;

import static org.junit.Assert.assertEquals;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.CLOUDWATCH;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.DYNAMODB;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.KINESIS;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import hudson.Extension;
import hudson.ExtensionList;
import io.jenkins.plugins.aws.kinesisconsumer.GlobalKinesisConfiguration;
import io.jenkins.plugins.aws.kinesisconsumer.KinesisConsumer;
import io.jenkins.plugins.aws.kinesisconsumer.KinesisConsumerModule;
import io.jenkins.plugins.aws.kinesisconsumer.KinesisStreamItem;
import io.jenkins.plugins.aws.kinesisconsumer.utils.WaitUtil;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.For;
import org.jvnet.hudson.test.JenkinsRule;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.CreateStreamRequest;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;
import software.amazon.awssdk.services.kinesis.model.StreamStatus;

@For(AWSKinesisStreamListener.class)
public class AWSKinesisStreamListenerTest {
  @Rule public JenkinsRule j = new JenkinsRule();

  public static final String LEASE_TABLE_NAME = "jenkins-kinesis-consumer-test-stream";
  private static final Duration STREAM_CREATION_TIMEOUT = Duration.ofSeconds(10);
  private static final Duration TABLE_CREATION_TIMEOUT = Duration.ofSeconds(30);
  private static final Duration RECORD_CONSUMED_TIMEOUT = Duration.ofMinutes(5);

  public static final String STREAM_NAME = "test-stream";
  private KinesisClient kinesisClient;

  private static final int LOCALSTACK_PORT = 4566;

  @ClassRule
  public static LocalStackContainer localstack =
      new LocalStackContainer(DockerImageName.parse("localstack" + "/localstack:0.12.17"))
          .withServices(DYNAMODB, KINESIS, CLOUDWATCH)
          .withEnv("USE_SSL", "true")
          .withExposedPorts(LOCALSTACK_PORT);

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
    localstack.start();
    globalKinesisConfiguration = initGlobalConfig();

    kinesisClient =
        KinesisClient.builder()
            .endpointOverride(localstack.getEndpointOverride(KINESIS))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                        localstack.getAccessKey(), localstack.getSecretKey())))
            .httpClient(ApacheHttpClient.create())
            .region(Region.of(localstack.getRegion()))
            .build();

    System.setProperty("endpoint", localstack.getEndpointOverride(KINESIS).toASCIIString());
    System.setProperty("region", localstack.getRegion());
    System.setProperty("aws.accessKeyId", localstack.getAccessKey());
    System.setProperty("aws.secretAccessKey", localstack.getSecretKey());

    injector = Guice.createInjector(new TestKinesisConsumerModule());
    kinesisConsumerFactory = injector.getInstance(KinesisConsumer.Factory.class);
    dynamoDbAsynClient = injector.getInstance(DynamoDbAsyncClient.class);
  }

  @After
  public void tearDown() {
    localstack.close();
  }

  @Extension
  public static class TestListener extends AWSKinesisStreamListener {
    private int recordReceivedCounter;

    public TestListener() { recordReceivedCounter = 0; }

    int getRecordReceivedCounter() { return recordReceivedCounter; }

    @Override
    public String getStreamName() {
      return STREAM_NAME;
    }

    @Override
    public void onReceive(byte[] byteRecord) { recordReceivedCounter++; }
  }

  @Test
  public void shouldConsumeAnEventPublishedToAStream() throws Exception {
    createStreamAndWait();
    KinesisConsumer kinesisConsumer = kinesisConsumerFactory.create(globalKinesisConfiguration);
    kinesisConsumer.start();

    waitForLeaseTable();

    ExtensionList <AWSKinesisStreamListener> extensionList =
        AWSKinesisStreamListener.getAllRegisteredListeners();
    assertEquals(1, extensionList.size());

    TestListener testListener = (TestListener) extensionList.get(0);

    sendKinesisRecord();

    WaitUtil.waitUntil(() -> testListener.getRecordReceivedCounter() == 1,
     RECORD_CONSUMED_TIMEOUT);
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
    KinesisStreamItem kinesisStreamItem = new KinesisStreamItem(STREAM_NAME,
    "TRIM_HORIZON");

    globalKinesisConfiguration.setKinesisStreamItems(Collections.singletonList(kinesisStreamItem));

    globalKinesisConfiguration.load();
    return globalKinesisConfiguration;
  }

  private void createStreamAndWait()
      throws InterruptedException {
    createStreamAsync(STREAM_NAME);
    WaitUtil.waitUntil(
        () ->
            kinesisClient
                .describeStream(DescribeStreamRequest.builder().streamName(STREAM_NAME).build())
                .streamDescription()
                .streamStatus()
                .equals(StreamStatus.ACTIVE), STREAM_CREATION_TIMEOUT);
  }

  private void waitForLeaseTable() throws InterruptedException {

    WaitUtil.waitUntil(
        () -> {
          try {
            ScanResponse sr =
                dynamoDbAsynClient.scan(ScanRequest.builder().tableName(LEASE_TABLE_NAME).build()).get();
            return !sr.items().isEmpty();
          } catch (ExecutionException | InterruptedException ignored) {
          }
          return false;
        }, TABLE_CREATION_TIMEOUT);
  }

  private void createStreamAsync(String streamName) {
    kinesisClient.createStream(
        CreateStreamRequest.builder().streamName(streamName).shardCount(1).build());
  }
}
