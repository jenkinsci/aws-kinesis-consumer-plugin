package io.jenkins.plugins.aws.kinesisconsumer;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import hudson.Extension;
import hudson.model.listeners.ItemListener;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager of stream connections
 *
 * @author Antonio Barone
 */
@Extension
@Singleton
public class KinesisConsumerManager extends ItemListener {
  private static final Logger LOGGER = LoggerFactory.getLogger(KinesisConsumerManager.class);
  private KinesisConsumer.Factory kinesisConsumerFactory;
  private final Map<String, KinesisConsumer> consumers = new ConcurrentHashMap<>();

  @Inject
  public KinesisConsumerManager(KinesisConsumer.Factory kinesisConsumerFactory) {
    this.kinesisConsumerFactory = kinesisConsumerFactory;
  }

  public KinesisConsumerManager() {}

  @Override
  public final void onLoaded() {
    LOGGER.info("Start all consumers");
    // TODO (JENKINS-66569): this should start consumers
    super.onLoaded();
  }

  @Override
  public final void onBeforeShutdown() {
    LOGGER.info("Shutting down all kinesis consumers");
    consumers.values().forEach(KinesisConsumer::shutdown);
    consumers.clear();
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
      LOGGER.info("Starting kinesis consumers for all configured streams");
      configuration
          .getKinesisStreamItems()
          .forEach(
              s ->
                  consumers
                      .computeIfAbsent(
                          s.getStreamName(), stream -> kinesisConsumerFactory.create(configuration, stream))
                      .subscribe());
    } else {
      LOGGER.info("NO kinesis consumers will be started as per configuration");
    }
  }

  @VisibleForTesting
  Map<String, KinesisConsumer> getKinesisConsumers() {
    return consumers;
  }
}
