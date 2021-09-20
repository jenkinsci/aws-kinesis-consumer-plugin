package io.jenkins.plugins.aws.kinesisconsumer;

import com.google.common.flogger.FluentLogger;
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

/**
 * Implements the {@link ShardRecordProcessor} interface to process data records fetched from Amazon
 * Kinesis
 *
 * @author Fabio Ponciroli
 */
public class KinesisRecordProcessor implements ShardRecordProcessor {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  public interface Factory {
    KinesisRecordProcessor create(String streamName);
  }

  public final String streamName;

  @AssistedInject
  KinesisRecordProcessor(@Assisted String streamName) {
    this.streamName = streamName;
  }

  @Override
  public void initialize(InitializationInput initializationInput) {
    logger.atInfo().log(
        "[streamName: %s] [shardId: %s] Initializing @ Sequence: %s",
        streamName, initializationInput.shardId(), initializationInput.extendedSequenceNumber());
  }

  /**
   * Forward each byte record of {@link ProcessRecordsInput} to the {@link AWSKinesisStreamListener}
   * interface
   *
   * @param processRecordsInput {@link ProcessRecordsInput} to process
   */
  @Override
  public void processRecords(ProcessRecordsInput processRecordsInput) {
    logger.atInfo().log(
        "[streamName: %s] Processing %s records", streamName, processRecordsInput.records().size());
    try {
      processRecordsInput
          .records()
          .forEach(
              consumerRecord -> {
                byte[] byteRecord = new byte[consumerRecord.data().remaining()];
                consumerRecord.data().get(byteRecord);
                AWSKinesisStreamListener.fireOnReceive(
                    streamName, new String(byteRecord));
              });
    } catch (Throwable t) {
      logger.atSevere().withCause(t).log(
          "[StreamName: %s] Caught throwable while processing records. Aborting.", streamName);
    }
  }

  @Override
  public void leaseLost(LeaseLostInput leaseLostInput) {
    logger.atInfo().log("[streamName: %s] lease lost", streamName);
  }

  @Override
  public void shardEnded(ShardEndedInput shardEndedInput) {
    try {
      logger.atInfo().log("[StreamName: %s] Reached shard end checkpointing.", streamName);
      shardEndedInput.checkpointer().checkpoint();
    } catch (ShutdownException | InvalidStateException e) {
      logger.atSevere().withCause(e).log(
          "[StreamName: %s] Exception while checkpointing at shard end. Giving up.", streamName);
    }
  }

  @Override
  public void shutdownRequested(ShutdownRequestedInput shutdownRequestedInput) {
    try {
      logger.atInfo().log(
          "[StreamName: %s] Scheduler is shutting down, checkpointing.", streamName);
      shutdownRequestedInput.checkpointer().checkpoint();
    } catch (ShutdownException | InvalidStateException e) {
      logger.atSevere().withCause(e).log(
          "[StreamName: %s] Exception while checkpointing at requested shutdown. Giving up.",
          streamName);
    }
  }
}
