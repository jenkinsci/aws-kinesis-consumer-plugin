package io.jenkins.plugins.aws.kinesisconsumer;

import com.google.common.flogger.FluentLogger;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import software.amazon.kinesis.coordinator.Scheduler;

/**
 * Responsible to connect to the configured Kinesis streams and start the scheduler threads to begin
 * polling records
 *
 * @author Fabio Ponciroli
 */
public class KinesisConsumer {
  public interface Factory {
    KinesisConsumer create(String streamName);
  }

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private Scheduler kinesisScheduler;
  private final SchedulerProvider.Factory schedulerProviderFactory;
  private final String streamName;

  @AssistedInject
  KinesisConsumer(SchedulerProvider.Factory schedulerProviderFactory, @Assisted String streamName) {
    this.schedulerProviderFactory = schedulerProviderFactory;
    this.streamName = streamName;
  }

  public void subscribe() {
    logger.atInfo().log("Launching NEW kinesis subscriber for stream %s", streamName);
    this.kinesisScheduler = schedulerProviderFactory.create(streamName).get();
    Thread schedulerThread = new Thread(kinesisScheduler);
    schedulerThread.setDaemon(true);
    schedulerThread.start();
  }

  /** Stop the scheduler threads to end consuming records from the Kinesis streams */
  public void shutdown() {
    // TODO: JENKINS-66590 Kinesis consumer fails shutting down workers
    logger.atInfo().log("Shutting down kinesis subscriber for stream %s", streamName);
    Future<Boolean> gracefulShutdownFuture = kinesisScheduler.startGracefulShutdown();
    try {
      gracefulShutdownFuture.get(10L, TimeUnit.MILLISECONDS);
    } catch (Exception e) {
      logger.atSevere().withCause(e).log(
          "Error shutting down kinesis subscriber for stream %s", streamName);
    }
  }
}
