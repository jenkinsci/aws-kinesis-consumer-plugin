package io.jenkins.plugins.aws.kinesisconsumer;

import static com.google.inject.Scopes.SINGLETON;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import hudson.Extension;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.kinesis.coordinator.Scheduler;

@Extension
public class KinesisConsumerModule extends AbstractModule {

  @Override
  protected void configure() {
    install(new FactoryModuleBuilder().build(KinesisRecordProcessor.Factory.class));
    install(new FactoryModuleBuilder().build(KinesisRecordProcessorFactory.Factory.class));
    bind(KinesisAsyncClient.class).toProvider(KinesisAsyncClientProvider.class).in(SINGLETON);
    bind(DynamoDbAsyncClient.class).toProvider(DynamoDbAsyncClientProvider.class).in(SINGLETON);
    bind(CloudWatchAsyncClient.class).toProvider(CloudWatchAsyncClientProvider.class).in(SINGLETON);
    bind(Scheduler.class).toProvider(SchedulerProvider.class).in(SINGLETON);
    install(new FactoryModuleBuilder().build(KinesisConsumer.Factory.class));
  }
}
