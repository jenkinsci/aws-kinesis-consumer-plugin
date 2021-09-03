package io.jenkins.plugins.aws.kinesisconsumer;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.CLOUDWATCH;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.DYNAMODB;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.KINESIS;

import java.time.Duration;
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
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.CreateStreamRequest;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamRequest;
import software.amazon.awssdk.services.kinesis.model.StreamStatus;

public class BaseLocalStack {
  @Rule public JenkinsRule j = new JenkinsRule();

  private static final Duration STREAM_CREATION_TIMEOUT = Duration.ofSeconds(10);
  private static final int LOCALSTACK_PORT = 4566;

  protected KinesisClient kinesisClient;

  @ClassRule
  public static LocalStackContainer localstack =
      new LocalStackContainer(DockerImageName.parse("localstack" + "/localstack:0.12.17"))
          .withServices(DYNAMODB, KINESIS, CLOUDWATCH)
          .withEnv("USE_SSL", "true")
          .withExposedPorts(LOCALSTACK_PORT);

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
}
