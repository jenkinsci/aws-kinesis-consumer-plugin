package io.jenkins.plugins.aws.kinesisconsumer;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import java.net.URI;
import org.apache.commons.lang.StringUtils;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClientBuilder;

/**
 * Provider of {@link KinesisAsyncClient}
 *
 * @author Fabio Ponciroli
 */
@Singleton
class KinesisAsyncClientProvider implements Provider<KinesisAsyncClient> {
  private final GlobalKinesisConfiguration globalKinesisConfiguration;

  @Inject
  KinesisAsyncClientProvider(GlobalKinesisConfiguration configuration) {
    this.globalKinesisConfiguration = configuration;
  }

  @Override
  public KinesisAsyncClient get() {
    KinesisAsyncClientBuilder builder = KinesisAsyncClient.builder();
    // TODO these checks can be refactored since they are common among CW, DDB
    // and KC
    if (StringUtils.isNotEmpty(globalKinesisConfiguration.getRegion())) {
      builder.region(Region.of(globalKinesisConfiguration.getRegion()));
    }
    if (StringUtils.isNotEmpty(globalKinesisConfiguration.getLocalEndpoint())) {
      try {
        builder.endpointOverride(new URI(globalKinesisConfiguration.getLocalEndpoint()));
      } catch (Exception e) {
      }
    }
    return builder.build();
  }
}
