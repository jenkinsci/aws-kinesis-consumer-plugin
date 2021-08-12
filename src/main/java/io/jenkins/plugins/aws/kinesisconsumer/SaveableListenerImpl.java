package io.jenkins.plugins.aws.kinesisconsumer;

import com.google.common.flogger.FluentLogger;
import com.google.inject.Inject;
import hudson.Extension;
import hudson.XmlFile;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;

@Extension
public class SaveableListenerImpl extends SaveableListener {

  private KinesisConsumer.Factory kinesisConsumerFactory;

  @Inject
  public SaveableListenerImpl(KinesisConsumer.Factory kinesisConsumerFactory) {
    this.kinesisConsumerFactory = kinesisConsumerFactory;
  }

  public SaveableListenerImpl() {}

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  @Override
  public final void onChange(Saveable o, XmlFile file) {
    if (o instanceof GlobalKinesisConfiguration) {
      logger.atInfo().log("AWS Kinesis Configuration is updated, restart consumer...");
      GlobalKinesisConfiguration newConfig = (GlobalKinesisConfiguration) o;
      KinesisConsumer kinesisConsumer = kinesisConsumerFactory.create(newConfig);
      kinesisConsumer.start();
    }
    super.onChange(o, file);
  }

  /**
   * Gets instance of this extension.
   *
   * @return the instance of this extension.
   */
  public static SaveableListenerImpl get() {
    return SaveableListener.all().get(SaveableListenerImpl.class);
  }
}
