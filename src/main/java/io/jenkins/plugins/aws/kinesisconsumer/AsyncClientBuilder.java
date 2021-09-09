package io.jenkins.plugins.aws.kinesisconsumer;

import com.google.common.flogger.FluentLogger;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.commons.lang.StringUtils;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.regions.Region;

/**
 * Base class for building AWS Async clients
 *
 * @author Antonio Barone
 */
class AsyncClientBuilder {
  protected static final FluentLogger logger = FluentLogger.forEnclosingClass();
  protected final GlobalKinesisConfiguration globalKinesisConfiguration;

  AsyncClientBuilder(GlobalKinesisConfiguration configuration) {
    this.globalKinesisConfiguration = configuration;
  }

  protected <T extends AwsClientBuilder<?, ?>> T baseConfig(T builder) {
    if (StringUtils.isNotEmpty(globalKinesisConfiguration.getRegion())) {
      builder.region(Region.of(globalKinesisConfiguration.getRegion()));
    }
    if (StringUtils.isNotEmpty(globalKinesisConfiguration.getLocalEndpoint())) {
      try {
        builder.endpointOverride(new URI(globalKinesisConfiguration.getLocalEndpoint()));
      } catch (URISyntaxException e) {
        logger.atSevere().withCause(e).log("Could not set local endpoint");
      }
    }
    return builder;
  }
}
