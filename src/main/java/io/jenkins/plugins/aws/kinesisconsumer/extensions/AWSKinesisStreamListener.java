package io.jenkins.plugins.aws.kinesisconsumer.extensions;

import com.google.common.flogger.FluentLogger;
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
 *       AWSKinesisStreamListener#onReceive(String, String)}
 * </ul>
 *
 * @author Fabio Ponciroli
 */
public abstract class AWSKinesisStreamListener implements ExtensionPoint {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  /**
   * This needs to be overridden to implement the logic upon record receive
   *
   * @param streamName source AWS Kinesis stream name
   * @param jsonPayload string containing the JSON payload of the AWS kinesis
   *  record
   */
  public abstract void onReceive(String streamName, String jsonPayload);

  public static void fireOnReceive(String streamName, String jsonPayload) {

    // TODO: Handle security: this way is deprecated
    SecurityContext old = ACL.impersonate(ACL.SYSTEM);
    try {
      for (AWSKinesisStreamListener listener : getAllRegisteredListeners()) {
        try {
          listener.onReceive(streamName, jsonPayload);
        } catch (Exception ex) {
          logger.atSevere().withCause(ex).log(
              "Error calling onReceive for " + "listener %s", listener);
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
