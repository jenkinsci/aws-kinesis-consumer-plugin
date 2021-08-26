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
  private String initialPositionInStream = null;

  /**
   * Creates KinesisStreamItem instance.
   *
   * @param streamName the queue name.
   */
  @DataBoundConstructor
  public KinesisStreamItem(String streamName, String initialPositionInStream) {
    this.streamName = StringUtils.stripToNull(streamName);
    this.initialPositionInStream = StringUtils.stripToNull(initialPositionInStream);
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

  /**
   * Get the initial position in the Kinesis stream Valid values are: LATEST or TRIM_HORIZON. Not
   * all the positions have been implemented for the time being.
   *
   * @see <a
   *     href="https://docs.aws.amazon.com/kinesis/latest/APIReference/API_StartingPosition.html">Kinesis
   *     Staring Positions</a>
   * @return the initial position in the Kinesis stream
   */
  public String getInitialPositionInStream() {
    return initialPositionInStream == null ? "TRIM_HORIZON" : initialPositionInStream;
  }

  /**
   * Set the initial position in the Kinesis stream
   *
   * @param initialPositionInStream initial position in the Kinesis stream
   */
  public void setInitialPositionInStream(String initialPositionInStream) {
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
