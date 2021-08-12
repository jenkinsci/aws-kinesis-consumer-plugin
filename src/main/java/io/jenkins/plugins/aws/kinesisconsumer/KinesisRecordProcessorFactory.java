package io.jenkins.plugins.aws.kinesisconsumer;

import com.google.inject.Inject;
import software.amazon.kinesis.common.StreamIdentifier;
import software.amazon.kinesis.processor.ShardRecordProcessor;
import software.amazon.kinesis.processor.ShardRecordProcessorFactory;

public class KinesisRecordProcessorFactory implements ShardRecordProcessorFactory {
  interface Factory {
    KinesisRecordProcessorFactory create();
  }

  private final KinesisRecordProcessor.Factory processorFactory;

  @Inject
  KinesisRecordProcessorFactory(KinesisRecordProcessor.Factory processorFactory) {
    this.processorFactory = processorFactory;
  }

  @Override
  public ShardRecordProcessor shardRecordProcessor() {
    // TODO this constructor shouldn't be allowed. Need to handle it
    return null;
  }

  @Override
  public ShardRecordProcessor shardRecordProcessor(StreamIdentifier streamIdentifier) {
    return new KinesisRecordProcessor(streamIdentifier.streamName());
  }
}
