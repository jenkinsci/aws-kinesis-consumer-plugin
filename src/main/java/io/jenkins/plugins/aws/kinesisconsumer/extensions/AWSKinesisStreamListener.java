package io.jenkins.plugins.aws.kinesisconsumer.extensions;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;

/**
 * Extension point to implement by external applications to listen to records coming from a Kinesis
 * stream. The external application has to:
 *
 * <ul>
 *   <li>Implement the logic upon record receive by overriding {@link
 *       AWSKinesisStreamListener#onReceive(String, byte[])}
 * </ul>
 *
 * @author Fabio Ponciroli
 */
public abstract class AWSKinesisStreamListener implements ExtensionPoint {
  /**
   * This needs to be overridden to implement the logic upon record receive
   *
   * @param streamName source AWS Kinesis stream name
   * @param byteRecord input byte record to process
   */
  public abstract void onReceive(String streamName, byte[] byteRecord);

  public static void fireOnReceive(String streamName, byte[] byteRecord) {

    // TODO: Handle security: this way is deprecated
    SecurityContext old = ACL.impersonate(ACL.SYSTEM);
    try {
      for (AWSKinesisStreamListener listener : getAllRegisteredListeners()) {
        try {
          listener.onReceive(streamName, byteRecord);
        } catch (Exception ex) {

        }
      }
    } finally {
      SecurityContextHolder.setContext(old);
    }
  }

  /**
   * Gets all listeners.
   *
   * @return the extension list.
   */
  public static ExtensionList<AWSKinesisStreamListener> getAllRegisteredListeners() {
    Jenkins jenkins = Jenkins.getInstanceOrNull();
    if (jenkins != null) {
      return jenkins.getExtensionList(AWSKinesisStreamListener.class);
    }

    throw new IllegalStateException("Jenkins is not started or is stopped");
  }
}
