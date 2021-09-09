package io.jenkins.plugins.aws.kinesisconsumer;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

/**
 * Provider of {@link CloudWatchAsyncClient}
 *
 * @author Fabio Ponciroli
 */
@Singleton
class CloudWatchAsyncClientProvider extends AsyncClientBuilder
    implements Provider<CloudWatchAsyncClient> {

  @Inject
  CloudWatchAsyncClientProvider(GlobalKinesisConfiguration configuration) {
    super(configuration);
  }

  @Override
  public CloudWatchAsyncClient get() {
    return baseConfig(CloudWatchAsyncClient.builder()).build();
  }
}
