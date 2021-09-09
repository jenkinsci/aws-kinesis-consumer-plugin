package io.jenkins.plugins.aws.kinesisconsumer;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

/**
 * Provider of {@link DynamoDbAsyncClient}
 *
 * @author Fabio Ponciroli
 */
@Singleton
class DynamoDbAsyncClientProvider extends AsyncClientBuilder
    implements Provider<DynamoDbAsyncClient> {

  @Inject
  DynamoDbAsyncClientProvider(GlobalKinesisConfiguration configuration) {
    super(configuration);
  }

  @Override
  public DynamoDbAsyncClient get() {
    return baseConfig(DynamoDbAsyncClient.builder()).build();
  }
}
