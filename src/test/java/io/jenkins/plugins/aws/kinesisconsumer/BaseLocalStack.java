package io.jenkins.plugins.aws.kinesisconsumer;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.CLOUDWATCH;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.DYNAMODB;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.KINESIS;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.jenkins.plugins.aws.kinesisconsumer.utils.WaitUtil;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.jvnet.hudson.test.JenkinsRule;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.CreateStreamRequest;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamRequest;
import software.amazon.awssdk.services.kinesis.model.StreamStatus;

public class BaseLocalStack {
  @Rule public JenkinsRule j = new JenkinsRule();

  protected static final String STREAM_NAME = "test-stream";
  protected static final String PROJECT_FIELD_NAME = "project";
  protected static final String LEASE_TABLE_NAME = "jenkins-kinesis-consumer-test-stream";
  protected static final Duration TABLE_CREATION_TIMEOUT = Duration.ofSeconds(60);
  protected static final Duration STREAM_CREATION_TIMEOUT = Duration.ofSeconds(10);
  protected static final Duration RECORD_CONSUMED_TIMEOUT = Duration.ofMinutes(5);
  protected static final int LOCALSTACK_PORT = 4566;

  protected static Injector injector;
  protected KinesisClient kinesisClient;
  protected DynamoDbAsyncClient dynamoDbAsynClient;
  protected KinesisConsumerManager kinesisConsumerManager;
  protected GlobalKinesisConfiguration globalKinesisConfiguration;

  @ClassRule
  public static LocalStackContainer localstack =
      new LocalStackContainer(DockerImageName.parse("localstack" + "/localstack:0.12.17"))
          .withServices(DYNAMODB, KINESIS, CLOUDWATCH)
          .withEnv("USE_SSL", "true")
          .withExposedPorts(LOCALSTACK_PORT);

  class TestKinesisConsumerModule extends AbstractModule {

    @Override
    protected void configure() {
      globalKinesisConfiguration = initGlobalConfig(STREAM_NAME, PROJECT_FIELD_NAME);
      bind(GlobalKinesisConfiguration.class).toInstance(globalKinesisConfiguration);
      install(new KinesisConsumerModule());
    }
  }

  @Before
  public void setUp() throws Exception {
    localstack.start();
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

    globalKinesisConfiguration = initGlobalConfig(STREAM_NAME, PROJECT_FIELD_NAME);
    injector = Guice.createInjector(new TestKinesisConsumerModule());
    dynamoDbAsynClient = injector.getInstance(DynamoDbAsyncClient.class);
    kinesisConsumerManager = injector.getInstance(KinesisConsumerManager.class);
  }

  @After
  public void tearDown() {
    localstack.close();
  }

  protected void createStreamAsync(String streamName) {
    kinesisClient.createStream(
        CreateStreamRequest.builder().streamName(streamName).shardCount(1).build());
  }

  protected void createStreamAndWait(String streamName) throws InterruptedException {
    createStreamAsync(streamName);
    io.jenkins.plugins.aws.kinesisconsumer.utils.WaitUtil.waitUntil(
        () ->
            kinesisClient
                .describeStream(DescribeStreamRequest.builder().streamName(streamName).build())
                .streamDescription()
                .streamStatus()
                .equals(StreamStatus.ACTIVE),
        STREAM_CREATION_TIMEOUT);
  }

  private GlobalKinesisConfiguration initGlobalConfig(String streamName,
   String projectFieldName) {
    GlobalKinesisConfiguration globalKinesisConfiguration = new GlobalKinesisConfiguration();
    globalKinesisConfiguration.setKinesisConsumerEnabled(true);
    globalKinesisConfiguration.setLocalEndpoint(
        localstack.getEndpointOverride(KINESIS).toASCIIString());
    globalKinesisConfiguration.setRegion(localstack.getRegion());
    globalKinesisConfiguration.setApplicationName("jenkins-kinesis-consumer");
    KinesisStreamItem kinesisStreamItem = new KinesisStreamItem(streamName,
    "TRIM_HORIZON", projectFieldName, true);

    globalKinesisConfiguration.setKinesisStreamItems(Collections.singletonList(kinesisStreamItem));

    globalKinesisConfiguration.load();
    return globalKinesisConfiguration;
  }

  protected void waitForLeaseTable() throws InterruptedException {

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
