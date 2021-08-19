package io.jenkins.plugins.aws.kinesisconsumer.extensions;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.security.ACL;
import java.util.Objects;
import jenkins.model.Jenkins;
import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;

/**
 * Extension point to implement by external applications to listen to records
 * coming from a Kinesis stream. The external application has to:
 * <ul>
 *  <il>Specify which stream to listen to overriding <code>getStreamName</code></il>
 *  <il>Implement the logic upon record receive by overriding <code>onReceive
 *  ()</code></il>
 * </ul>
 *
 * @author Fabio Ponciroli
 */
public abstract class AWSKinesisStreamListener implements ExtensionPoint {

  /**
   * This needs to be overridden to specify which Kinesis record to listen
   *
   * @return Kinesis stream name to listen
   */
  public abstract String getStreamName();

  /**
   * This needs to be overridden to implement the logic upon record receive
   *
   * @param byteRecord input byte record to process
   */
  public abstract void onReceive(byte[] byteRecord);

  public static void fireOnReceive(String streamName, byte[] byteRecord) {

    // TODO: Handle security: this way is deprecated
    SecurityContext old = ACL.impersonate(ACL.SYSTEM);
    try {
      for (AWSKinesisStreamListener listener : getAllRegisteredListeners()) {
        if (streamName.equals(listener.getStreamName())) {
          try {
            listener.onReceive(byteRecord);
          } catch (Exception ex) {

          }
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
    return Objects.requireNonNull(Jenkins.getInstanceOrNull())
        .getExtensionList(AWSKinesisStreamListener.class);
  }
}
