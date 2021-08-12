package io.jenkins.plugins.aws.kinesisconsumer;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Class representing a Stream configuration
 *
 * @author Fabio Ponciroli
 */
public class KinesisStreamItem implements Describable<KinesisStreamItem> {
  private String streamName = null;

  /**
   * Creates KinesisStreamItem instance.
   *
   * @param streamName the queue name.
   */
  @DataBoundConstructor
  public KinesisStreamItem(String streamName) {
    this.streamName = StringUtils.stripToNull(streamName);
  }

  /**
   * Get the stream name
   *
   * @return the stream name
   */
  public String getStreamName() {
    return streamName;
  }

  /**
   * Set the stram name
   *
   * @param streamName stream name
   */
  public void setStreamName(String streamName) {
    this.streamName = streamName;
  }

  @Extension
  public static class DescriptorImpl extends Descriptor<KinesisStreamItem> {
    @Override
    public String getDisplayName() {
      return "";
    }
  }

  /**
   * Get a Descriptor<KinesisStreamItem> instance
   *
   * @return a Descriptor<KinesisStreamItem> instance
   */
  @Override
  @NonNull
  public Descriptor<KinesisStreamItem> getDescriptor() {
    return (Descriptor<KinesisStreamItem>) Jenkins.get().getDescriptorOrDie(getClass());
  }
}
