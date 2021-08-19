package io.jenkins.plugins.aws.kinesisconsumer;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import java.net.URI;
import org.apache.commons.lang.StringUtils;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClientBuilder;

/**
 * Provider of <class>CloudWatchAsyncClient</class>
 *
 * @author Fabio Ponciroli
 */
@Singleton
class CloudWatchAsyncClientProvider implements Provider<CloudWatchAsyncClient> {
  private final GlobalKinesisConfiguration globalKinesisConfiguration;

  @Inject
  CloudWatchAsyncClientProvider(GlobalKinesisConfiguration configuration) {
    this.globalKinesisConfiguration = configuration;
  }

  @Override
  public CloudWatchAsyncClient get() {
    CloudWatchAsyncClientBuilder builder = CloudWatchAsyncClient.builder();
    // TODO these checks can be refactored since they are common among CW, DDB
    // and KC
    if (StringUtils.isNotEmpty(globalKinesisConfiguration.getRegion())) {
      builder.region(Region.of(globalKinesisConfiguration.getRegion()));
    }
    if (StringUtils.isNotEmpty(globalKinesisConfiguration.getLocalEndpoint())) {
      try {
        builder.endpointOverride(new URI(globalKinesisConfiguration.getLocalEndpoint()));
      } catch (Exception e) {
        // TODO handle exception
      }
    }
    return builder.build();
  }
}
