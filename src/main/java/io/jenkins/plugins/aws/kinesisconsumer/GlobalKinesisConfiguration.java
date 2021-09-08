package io.jenkins.plugins.aws.kinesisconsumer;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.util.FormValidation;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.kinesis.common.InitialPositionInStream;

/**
 * Class representing the Global Kinesis configuration
 *
 * @author Fabio Ponciroli
 */
@Extension
@Symbol("aws-kinesis-consumer")
public class GlobalKinesisConfiguration extends GlobalConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(GlobalKinesisConfiguration.class);

  private static final UrlValidator URL_VALIDATOR = new UrlValidator(UrlValidator.ALLOW_LOCAL_URLS);

  private boolean kinesisConsumerEnabled;
  private String region;
  private List<KinesisStreamItem> kinesisStreamItems;
  private String localEndpoint;
  private String applicationName;

  /**
   * Set AWS Region when loading the global configuration page
   *
   * @param regionString AWS Region to connect to. This parameter is optional. If set it will
   *     override any Region set in the Region Provider Chain.
   */
  @DataBoundSetter
  public void setRegion(String regionString) {
    this.region = regionString;
  }

  /**
   * Set application name when loading the global configuration page This name identifies the
   * application, which must have a unique name that is scoped to the Amazon account and Region used
   * by the application. This name is used as a name for the control table in Amazon DynamoDB and
   * the namespace for Amazon CloudWatch metrics.
   *
   * @param applicationNameString the name of this application
   */
  @DataBoundSetter
  public void setApplicationName(String applicationNameString) {
    this.applicationName = applicationNameString;
  }

  /**
   * Set the enabled/disabled status of the plugin when loading the global configuration page
   *
   * @param kinesisConsumerEnabled enabled/disabled status
   */
  @DataBoundSetter
  public void setKinesisConsumerEnabled(boolean kinesisConsumerEnabled) {
    this.kinesisConsumerEnabled = kinesisConsumerEnabled;
  }

  /**
   * Set KinesisStreamItem values when loading the global configuration page
   *
   * @param kinesisStreamItems KinesisStreamItem to set
   */
  @DataBoundSetter
  public void setKinesisStreamItems(List<KinesisStreamItem> kinesisStreamItems) {
    this.kinesisStreamItems = kinesisStreamItems;
  }

  /**
   * Set an optional endpoint to point to a local kinesis stack rather than the AWS service. Useful
   * for development.
   *
   * @param localEndpoint the local endpoint URL, i.e. http://localhost:4566
   */
  @DataBoundSetter
  public void setLocalEndpoint(String localEndpoint) {
    this.localEndpoint = localEndpoint;
  }

  public GlobalKinesisConfiguration() {
    load();
  }

  /**
   * Get the GlobalKinesisConfiguration
   *
   * @return the global Kinesis configuration
   */
  public static GlobalKinesisConfiguration get() {
    return ExtensionList.lookupSingleton(GlobalKinesisConfiguration.class);
  }

  /**
   * Indicates if the plugin is enabled or not
   *
   * @return a boolean indicating the status of the plugin
   */
  public boolean isKinesisConsumerEnabled() {
    return kinesisConsumerEnabled;
  }

  /**
   * Get the AWS Region to connect to from the configuration page
   *
   * @return AWS Region to connect to
   */
  public String getRegion() {
    return region;
  }

  /**
   * Get the application name from the configuration page
   *
   * @return Application name
   */
  public String getApplicationName() {
    return applicationName;
  }

  public List<KinesisStreamItem> getKinesisStreamItems() {
    return Optional.ofNullable(kinesisStreamItems).orElse(Collections.emptyList());
  }

  /**
   * Get the local endpoint to consume from rather than the AWS service
   *
   * @return The local endpoint, or null, when not defined.
   */
  public String getLocalEndpoint() {
    return localEndpoint;
  }

  /**
   * Checks local endpoint URL is valid.
   *
   * @param value the URL.
   * @return FormValidation object that indicates ok or error.
   */
  public FormValidation doCheckLocalEndpoint(@QueryParameter String value) {
    String val = StringUtils.stripToNull(value);
    if (val == null) {
      return FormValidation.ok();
    }

    if (URL_VALIDATOR.isValid(val)) {
      return FormValidation.ok();
    } else {
      String errorMessage = String.format("'%s' is not a valid URL", value);
      LOGGER.error(errorMessage);
      return FormValidation.error(errorMessage);
    }
  }

  /**
   * Checks AWS region is valid.
   *
   * @param value the region.
   * @return FormValidation object that indicates ok or error.
   */
  public FormValidation doCheckRegion(@QueryParameter String value) {
    String val = StringUtils.stripToNull(value);
    if (val == null) {
      return FormValidation.ok();
    }

    if (Region.regions().contains(Region.of(value))) {
      return FormValidation.ok();
    } else {
      String errorMessage =
          String.format(
              "'%s' is not a valid AWS region. Valid regions are: %s",
              region,
              Region.regions().stream().map(Region::toString).collect(Collectors.joining(",")));
      LOGGER.error(errorMessage);
      return FormValidation.error(errorMessage);
    }
  }

  /**
   * Checks applicationName is valid
   *
   * @param value the application name
   * @return FormValidation object that indicates ok or error.
   */
  public FormValidation doCheckApplicationName(@QueryParameter String value) {
    String val = StringUtils.stripToNull(value);
    if (val == null) {
      return FormValidation.error("Application name is required");
    }
    return FormValidation.ok();
  }

  /**
   * Checks AWS Kinesis initial stream position is valid.
   *
   * @param value the initial position in the stream. Valid values: LATEST, TRIM_HORIZON
   * @return FormValidation object that indicates ok or error.
   */
  public FormValidation doCheckInitialPositionInStream(@QueryParameter String value) {
    String val = StringUtils.stripToNull(value);
    if (val == null) {
      return FormValidation.ok();
    }

    if (value.equalsIgnoreCase(InitialPositionInStream.TRIM_HORIZON.toString())
        || value.equalsIgnoreCase(InitialPositionInStream.LATEST.toString()))
      return FormValidation.ok();

    String errorMessage =
        String.format(
            "'%s' is not a valid initial position. Valid positions:" + " TRIM_HORIZON, LATEST",
            value);
    LOGGER.error(errorMessage);
    return FormValidation.error(errorMessage);
  }

  /**
   * @param req {@link StaplerRequest} submitted when saving the configuration page
   * @param json JSON containing the configuration parameters set
   * @return true if the configuration was correctly saved, false otherwise
   */
  @Override
  public boolean configure(StaplerRequest req, JSONObject json) {
    if (kinesisStreamItems != null) {
      kinesisStreamItems.clear();
    }
    req.bindJSON(this, json);
    save();
    return true;
  }

  public KinesisStreamItem getKinesisStreamItemsForStream(String streamName) {
    return getKinesisStreamItems().stream()
        .filter(s -> streamName.equals(s.getStreamName()))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    String.format("Could not find stream %s", streamName)));
  }
}
