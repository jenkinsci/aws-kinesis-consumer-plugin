package io.jenkins.plugins.aws.kinesisconsumer;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import java.net.URI;
import org.apache.commons.lang.StringUtils;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClientBuilder;

@Singleton
class DynamoDbAsyncClientProvider implements Provider<DynamoDbAsyncClient> {
  private final GlobalKinesisConfiguration globalKinesisConfiguration;

  @Inject
  DynamoDbAsyncClientProvider(GlobalKinesisConfiguration configuration) {
    this.globalKinesisConfiguration = configuration;
  }

  @Override
  public DynamoDbAsyncClient get() {
    DynamoDbAsyncClientBuilder builder = DynamoDbAsyncClient.builder();
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
