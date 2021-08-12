package io.jenkins.plugins.aws.kinesisconsumer.extensions;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.security.ACL;
import java.util.Objects;
import jenkins.model.Jenkins;
import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;

public abstract class AWSKinesisStreamListener implements ExtensionPoint {

  public abstract String getStreamName();

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
