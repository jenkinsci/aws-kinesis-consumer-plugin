package io.jenkins.plugins.aws.kinesisconsumer;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import hudson.Extension;
import hudson.model.listeners.ItemListener;
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

  private KinesisConsumer
      kinesisConsumer; // TODO: JENKINS-66496 - should actually connect to multiple streams

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
    if (kinesisConsumer != null) {
      LOGGER.info("Shutting down all kinesis consumers");
      kinesisConsumer.shutdown();
    }
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

  public void start(GlobalKinesisConfiguration configuration) {
    kinesisConsumer = kinesisConsumerFactory.create(configuration);
    kinesisConsumer.start();
  }

  @VisibleForTesting
  KinesisConsumer getKinesisConsumer() {
    return kinesisConsumer;
  }
}
