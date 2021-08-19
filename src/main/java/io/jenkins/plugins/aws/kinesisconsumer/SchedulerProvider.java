package io.jenkins.plugins.aws.kinesisconsumer;

import com.google.inject.Inject;
import com.google.inject.Provider;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.kinesis.common.ConfigsBuilder;
import software.amazon.kinesis.coordinator.Scheduler;
import software.amazon.kinesis.retrieval.RetrievalConfig;

/**
*  Provider of Kinesis Scheduler
*
*  @author Fabio Ponciroli
**/
public class SchedulerProvider implements Provider<Scheduler> {

  private final GlobalKinesisConfiguration configuration;
  private final KinesisAsyncClient kinesisAsyncClient;
  private final DynamoDbAsyncClient dynamoDbAsyncClient;
  private final CloudWatchAsyncClient cloudWatchAsyncClient;
  private final KinesisRecordProcessorFactory kinesisRecordProcessorFactory;
  private ConfigsBuilder configsBuilder;
  private RetrievalConfig retrievalConfig;
  private String streamName;
  // TODO this should be configurable
  public static final String APPLICATION_NAME = "jenkins-kinesis-consumer";

  @Inject
  public SchedulerProvider(
      GlobalKinesisConfiguration configuration,
      KinesisAsyncClient kinesisAsyncClient,
      DynamoDbAsyncClient dynamoDbAsyncClient,
      CloudWatchAsyncClient cloudWatchAsyncClient,
      KinesisRecordProcessorFactory kinesisRecordProcessorFactory) {
    this.configuration = configuration;
    this.kinesisAsyncClient = kinesisAsyncClient;
    this.dynamoDbAsyncClient = dynamoDbAsyncClient;
    this.cloudWatchAsyncClient = cloudWatchAsyncClient;
    this.kinesisRecordProcessorFactory = kinesisRecordProcessorFactory;
  }

  /**
   * Enrich the SchedulerProvider with stream information
   *
   * @param streamName stream to create the provider for
   * @return a SchedulerProvider for a given stream
   */
  public SchedulerProvider forStream(String streamName) {
    this.streamName = streamName;

    this.configsBuilder =
        new ConfigsBuilder(
            streamName,
            cosumerLeaseName(APPLICATION_NAME, streamName),
            kinesisAsyncClient,
            dynamoDbAsyncClient,
            cloudWatchAsyncClient,
            getWorkerIdentifier(streamName),
            kinesisRecordProcessorFactory);
    // TODO This should read from configuration
    this.retrievalConfig = configsBuilder.retrievalConfig();
    return this;
  }

  /**
   * Get an instance of Kinesis Sheduler from the provider
   *
   * @return a Kinesis Scheduler
   */
  @Override
  public Scheduler get() {
    return new Scheduler(
        configsBuilder.checkpointConfig(),
        configsBuilder.coordinatorConfig(),
        configsBuilder.leaseManagementConfig(),
        configsBuilder.lifecycleConfig(),
        configsBuilder.metricsConfig(),
        configsBuilder.processorConfig(),
        retrievalConfig);
  }

  private static String getWorkerIdentifier(String streamName) {
    return String.format("klc-worker-%s-%s", APPLICATION_NAME, streamName);
  }

  private static String cosumerLeaseName(String applicationName, String streamName) {
    return String.format("%s-%s", applicationName, streamName);
  }
}
