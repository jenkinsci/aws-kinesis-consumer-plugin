package io.jenkins.plugins.aws.kinesisconsumer;

import com.google.common.flogger.FluentLogger;
import com.google.inject.Inject;
import hudson.Extension;
import hudson.XmlFile;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;

/**
 * Extends {@link SaveableListener} to listen to {@link GlobalKinesisConfiguration} changes.
 *
 * @author Fabio Ponciroli
 */
@Extension
public class SaveableListenerImpl extends SaveableListener {
  private KinesisConsumerManager manager;

  @Inject
  public SaveableListenerImpl(KinesisConsumerManager manager) {
    this.manager = manager;
  }

  public SaveableListenerImpl() {}

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  /**
   * On change restart the AWS Kinesis consumer. This is useful, for example, when adding a new
   * stream to listen from
   *
   * @param o saved object
   * @param file XML file containing the new configuration
   */
  @Override
  public final void onChange(Saveable o, XmlFile file) {
    if (o instanceof GlobalKinesisConfiguration) {
      // TODO: JENKINS-66591 kinesis consumer should restart workers upon configuration change
      logger.atInfo().log("AWS Kinesis Configuration is updated, restart consumer...");
      manager.startAllConsumers((GlobalKinesisConfiguration) o);
    }
    super.onChange(o, file);
  }

  /**
   * Gets an instance of this extension
   *
   * @return an instance of this extension
   */
  public static SaveableListenerImpl get() {
    return SaveableListener.all().get(SaveableListenerImpl.class);
  }
}
