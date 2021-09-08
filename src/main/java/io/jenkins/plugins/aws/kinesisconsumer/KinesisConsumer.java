package io.jenkins.plugins.aws.kinesisconsumer;

import com.google.common.annotations.VisibleForTesting;
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
    KinesisConsumer create(GlobalKinesisConfiguration configuration, String streamName);
  }

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private Scheduler kinesisScheduler;
  private boolean isStarted = false;
  private final SchedulerProvider.Factory schedulerProviderFactory;
    private final GlobalKinesisConfiguration configuration;
    private final String streamName;

  @AssistedInject
  KinesisConsumer(SchedulerProvider.Factory schedulerProviderFactory, @Assisted GlobalKinesisConfiguration configuration, @Assisted String streamName) {
    this.schedulerProviderFactory = schedulerProviderFactory;
    this.configuration = configuration;
    this.streamName = streamName;
  }

  public void subscribe() {
    logger.atInfo().log("Launching NEW kinesis subscriber for stream %s", streamName);
    this.kinesisScheduler = schedulerProviderFactory.create(configuration, streamName).get();
    Thread schedulerThread = new Thread(kinesisScheduler);
    schedulerThread.setDaemon(true);
    schedulerThread.start();
    isStarted = true;
  }

  /** Stop the scheduler threads to end consuming records from the Kinesis streams */
  public void shutdown() {
    if (isStarted) {
        logger.atInfo().log("Shutting down kinesis subscriber for stream %s", streamName);
        Future<Boolean> gracefulShutdownFuture = kinesisScheduler.startGracefulShutdown();
      try {
        gracefulShutdownFuture.get(10L, TimeUnit.MILLISECONDS);
      } catch (Exception e) {
          logger.atSevere().withCause(e).log(
                  "Error shutting down kinesis subscriber for stream %s", streamName);
      } finally {
        isStarted = false;
      }
    }
  }

  @VisibleForTesting
  boolean isStarted() {
    return isStarted;
  }
}
