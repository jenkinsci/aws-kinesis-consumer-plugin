package io.jenkins.plugins.aws.kinesisconsumer;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.flogger.FluentLogger;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import hudson.Extension;
import hudson.model.listeners.ItemListener;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager of stream connections
 *
 * @author Antonio Barone
 */
@Extension
@Singleton
public class KinesisConsumerManager extends ItemListener {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private GlobalKinesisConfiguration configuration;
  private KinesisConsumer.Factory kinesisConsumerFactory;
  private final Map<String, KinesisConsumer> consumers = new ConcurrentHashMap<>();

  @Inject
  public KinesisConsumerManager(
      GlobalKinesisConfiguration configuration, KinesisConsumer.Factory kinesisConsumerFactory) {
    this.configuration = configuration;
    this.kinesisConsumerFactory = kinesisConsumerFactory;
  }

  public KinesisConsumerManager() {}

  @Override
  public final void onLoaded() {
    startAllConsumers(configuration);
    super.onLoaded();
  }

  @Override
  public final void onBeforeShutdown() {
    shutDownAllConsumers();
    super.onBeforeShutdown();
  }

  /**
   * Gets this extension's instance.
   *
   * @return the instance of this extension.
   */
  public static KinesisConsumerManager get() {
    return ItemListener.all().get(KinesisConsumerManager.class);
  }

  public void startAllConsumers(GlobalKinesisConfiguration configuration) {
    if (configuration != null
        && configuration.isKinesisConsumerEnabled()
        && !configuration.getKinesisStreamItems().isEmpty()) {
      logger.atInfo().log("Starting kinesis consumers for all configured streams");
      configuration
          .getKinesisStreamItems()
          .forEach(
              s ->
                  consumers
                      .computeIfAbsent(
                          s.getStreamName(),
                          stream -> kinesisConsumerFactory.create(configuration, stream))
                      .subscribe());
    } else {
      logger.atInfo().log("NO kinesis consumers will be started as per configuration");
    }
  }

  @VisibleForTesting
  Map<String, KinesisConsumer> getKinesisConsumers() {
    return consumers;
  }

  public void shutDownAllConsumers() {
    logger.atInfo().log("Shutting down all kinesis consumers");
    consumers.values().forEach(KinesisConsumer::shutdown);
    consumers.clear();
  }

  public void restartAllConsumers(GlobalKinesisConfiguration configuration) {
    logger.atInfo().log("Restarting all kinesis consumers");
    shutDownAllConsumers();
    startAllConsumers(configuration);
  }
}
