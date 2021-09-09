package io.jenkins.plugins.aws.kinesisconsumer;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;

/**
 * Provider of {@link KinesisAsyncClient}
 *
 * @author Fabio Ponciroli
 */
@Singleton
class KinesisAsyncClientProvider extends AsyncClientBuilder
    implements Provider<KinesisAsyncClient> {

  @Inject
  KinesisAsyncClientProvider(GlobalKinesisConfiguration configuration) {
    super(configuration);
  }

  @Override
  public KinesisAsyncClient get() {
    return baseConfig(KinesisAsyncClient.builder()).build();
  }
}
