package io.jenkins.plugins.aws.kinesisconsumer;

import static software.amazon.awssdk.services.kinesis.model.StreamStatus.ACTIVE;
import static software.amazon.awssdk.services.kinesis.model.StreamStatus.CREATING;
import static software.amazon.awssdk.services.kinesis.model.StreamStatus.UPDATING;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Util;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import jenkins.model.Jenkins;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClientBuilder;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamRequest;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamResponse;
import software.amazon.awssdk.services.kinesis.model.StreamStatus;

/**
 * Class representing a Stream configuration
 *
 * @author Fabio Ponciroli
 */
public class KinesisStreamItem implements Describable<KinesisStreamItem> {
  private String streamName = null;
  private String initialPositionInStream = null;
  private String projectNameField = null;
  private Boolean triggerSCMBuild = null;

  /**
   * Creates KinesisStreamItem instance.
   *
   * @param streamName the queue name.
   */
  @DataBoundConstructor
  public KinesisStreamItem(String streamName, String initialPositionInStream,
   String projectNameField, Boolean triggerSCMBuild) {
    this.streamName = StringUtils.stripToNull(streamName);
    this.initialPositionInStream = StringUtils.stripToNull(initialPositionInStream);
    this.projectNameField = StringUtils.stripToNull(projectNameField);
    this.triggerSCMBuild = triggerSCMBuild;
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
   * Set the stream name
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
   * @return the initial position in the Kinesis stream
   * @see <a
   *     href="https://docs.aws.amazon.com/kinesis/latest/APIReference/API_StartingPosition.html">Kinesis
   *     Staring Positions</a>
   */
  public String getInitialPositionInStream() {
    return initialPositionInStream == null ? "LATEST" : initialPositionInStream;
  }

  /**
   * Set the initial position in the Kinesis stream
   *
   * @param initialPositionInStream initial position in the Kinesis stream
   */
  public void setInitialPositionInStream(String initialPositionInStream) {
    this.initialPositionInStream = initialPositionInStream;
  }

  public String getProjectNameField() {
    return projectNameField;
  }

  public void setProjectNameField(String projectNameField) {
    this.projectNameField = projectNameField;
  }

  public Boolean getTriggerSCMBuild() {
    return triggerSCMBuild;
  }

  public void setTriggerSCMBuild(Boolean triggerSCMBuild) {
    this.triggerSCMBuild = triggerSCMBuild;
  }

  @Extension
  public static class DescriptorImpl extends Descriptor<KinesisStreamItem> {
    @Override
    public String getDisplayName() {
      return "";
    }

    @POST
    public FormValidation doTestConnection(
        @QueryParameter("region") final String region,
        @QueryParameter("localEndpoint") final String localEndpoint,
        @QueryParameter("streamName") final String streamName) {
      try {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);

        KinesisAsyncClient kinesisAsyncClient = kinesisAsyncClient(region, localEndpoint);
        DescribeStreamResponse describeStream =
            kinesisAsyncClient
                .describeStream(DescribeStreamRequest.builder().streamName(streamName).build())
                .get();

        StreamStatus streamStatus = describeStream.streamDescription().streamStatus();
        String streamARN = describeStream.streamDescription().streamARN();
        if (Arrays.asList(CREATING, ACTIVE, UPDATING).contains(streamStatus)) {
          return FormValidation.ok(
              "Success: stream %s (arn: %s). Status %s", streamName, streamARN, streamStatus);
        }
        return FormValidation.error(
            "Failure: stream %s (arn: %s). Status %s", streamName, streamARN, streamStatus);

      } catch (Exception e) {
        return FormValidation.error("Failed: " + e.getMessage());
      }
    }

    private KinesisAsyncClient kinesisAsyncClient(String region, String localEndpoint)
        throws URISyntaxException {
      KinesisAsyncClientBuilder builder = KinesisAsyncClient.builder();

      if (Util.fixEmptyAndTrim(region) != null) {
        builder.region(Region.of(region));
      }
      if (Util.fixEmptyAndTrim(localEndpoint) != null) {
        builder.endpointOverride(new URI(localEndpoint));
      }
      return builder.build();
    }
  }

  /**
   * Get a {@link Descriptor} of {@link KinesisStreamItem} instance
   *
   * @return a {@link Descriptor} of {@link KinesisStreamItem} instance
   */
  @Override
  @NonNull
  public Descriptor<KinesisStreamItem> getDescriptor() {
    return (Descriptor<KinesisStreamItem>) Jenkins.get().getDescriptorOrDie(getClass());
  }
}
