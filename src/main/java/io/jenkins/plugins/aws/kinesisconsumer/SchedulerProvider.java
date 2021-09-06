package io.jenkins.plugins.aws.kinesisconsumer;

import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.kinesis.common.ConfigsBuilder;
import software.amazon.kinesis.common.InitialPositionInStream;
import software.amazon.kinesis.common.InitialPositionInStreamExtended;
import software.amazon.kinesis.coordinator.Scheduler;
import software.amazon.kinesis.retrieval.RetrievalConfig;

/**
 * Provider of Kinesis Scheduler
 *
 * @author Fabio Ponciroli
 */
class SchedulerProvider implements Provider<Scheduler> {
  private final GlobalKinesisConfiguration configuration;
  private final KinesisAsyncClient kinesisAsyncClient;

  interface Factory {
    SchedulerProvider create(String streamName);
  }

  private ConfigsBuilder configsBuilder;

  private String streamName;

  @AssistedInject
  SchedulerProvider(
      GlobalKinesisConfiguration configuration,
      KinesisAsyncClient kinesisAsyncClient,
      DynamoDbAsyncClient dynamoDbAsyncClient,
      CloudWatchAsyncClient cloudWatchAsyncClient,
      KinesisRecordProcessorFactory.Factory kinesisRecordProcessorFactoryFactory,
      @Assisted String streamName) {
    this.configuration = configuration;
    this.streamName = streamName;
    this.kinesisAsyncClient = kinesisAsyncClient;

    this.configsBuilder =
        new ConfigsBuilder(
            streamName,
            cosumerLeaseName(streamName),
            kinesisAsyncClient,
            dynamoDbAsyncClient,
            cloudWatchAsyncClient,
            getWorkerIdentifier(streamName),
            kinesisRecordProcessorFactoryFactory.create(streamName));
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
        getRetrievalConfig());
  }

  private RetrievalConfig getRetrievalConfig() {
    RetrievalConfig retrievalConfig = configsBuilder.retrievalConfig();
    retrievalConfig.initialPositionInStreamExtended(
        InitialPositionInStreamExtended.newInitialPosition(
            InitialPositionInStream.valueOf(
                configuration
                    .getKinesisStreamItemsForStream(streamName)
                    .getInitialPositionInStream())));
    return retrievalConfig;
  }

  private String getWorkerIdentifier(String streamName) {
    return String.format("klc-worker-%s-%s", configuration.getApplicationName(), streamName);
  }

  private String cosumerLeaseName(String streamName) {
    return String.format("%s-%s", configuration.getApplicationName(), streamName);
  }
}
