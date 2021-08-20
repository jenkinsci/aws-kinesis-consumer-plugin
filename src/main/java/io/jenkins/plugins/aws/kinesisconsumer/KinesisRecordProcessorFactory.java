package io.jenkins.plugins.aws.kinesisconsumer;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import software.amazon.kinesis.processor.ShardRecordProcessor;
import software.amazon.kinesis.processor.ShardRecordProcessorFactory;

/**
 * Factory to create <code>KinesisRecordProcessor</code> instances.
 * The Kinesis Client Library will instantiate <code>KinesisRecordProcessor</code>
 * to process data records fetched from Kinesis.
 *
 * @author Fabio Ponciroli
 */
public class KinesisRecordProcessorFactory implements ShardRecordProcessorFactory {
  interface Factory {
    KinesisRecordProcessorFactory create(String streamName);
  }

  private final String streamName;
  private final KinesisRecordProcessor.Factory kinesisRecordProcessorFactory;

  @Inject
  KinesisRecordProcessorFactory(
      @Assisted String streamName, KinesisRecordProcessor.Factory kinesisRecordProcessorFactory) {
    this.streamName = streamName;
    this.kinesisRecordProcessorFactory = kinesisRecordProcessorFactory;
  }

  @Override
  public ShardRecordProcessor shardRecordProcessor() {
    return kinesisRecordProcessorFactory.create(streamName);
  }
}
