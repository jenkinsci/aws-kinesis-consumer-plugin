package io.jenkins.plugins.aws.kinesisconsumer;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import io.jenkins.plugins.aws.kinesisconsumer.extensions.AWSKinesisStreamListener;
import software.amazon.kinesis.exceptions.InvalidStateException;
import software.amazon.kinesis.exceptions.ShutdownException;
import software.amazon.kinesis.lifecycle.events.InitializationInput;
import software.amazon.kinesis.lifecycle.events.LeaseLostInput;
import software.amazon.kinesis.lifecycle.events.ProcessRecordsInput;
import software.amazon.kinesis.lifecycle.events.ShardEndedInput;
import software.amazon.kinesis.lifecycle.events.ShutdownRequestedInput;
import software.amazon.kinesis.processor.ShardRecordProcessor;

public class KinesisRecordProcessor implements ShardRecordProcessor {
  interface Factory {
    KinesisRecordProcessor create(String streamName);
  }

  public final String streamName;

  @AssistedInject
  KinesisRecordProcessor(@Assisted String streamName) {
    this.streamName = streamName;
  }

  @Override
  public void initialize(InitializationInput initializationInput) {
    // TODO handle exception
  }

  @Override
  public void processRecords(ProcessRecordsInput processRecordsInput) {
    try {
      processRecordsInput
          .records()
          .forEach(
              consumerRecord -> {
                AWSKinesisStreamListener.fireOnReceive(
                    streamName, new byte[consumerRecord.data().remaining()]);
              });
    } catch (Throwable t) {
      // TODO handle exception
    }
  }

  @Override
  public void leaseLost(LeaseLostInput leaseLostInput) {
    // TODO Add logging
  }

  @Override
  public void shardEnded(ShardEndedInput shardEndedInput) {
    try {
      shardEndedInput.checkpointer().checkpoint();
    } catch (ShutdownException | InvalidStateException e) {
      // TODO handle exception
    }
  }

  @Override
  public void shutdownRequested(ShutdownRequestedInput shutdownRequestedInput) {
    try {
      shutdownRequestedInput.checkpointer().checkpoint();
    } catch (ShutdownException | InvalidStateException e) {
      // TODO handle exception
    }
  }
}
