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
    KinesisConsumer create(GlobalKinesisConfiguration configuration);
  }

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private String streamName;
  private Scheduler kinesisScheduler;
  private final SchedulerProvider.Factory schedulerProviderFactory;
  private GlobalKinesisConfiguration configuration;

  @AssistedInject
  KinesisConsumer(
      SchedulerProvider.Factory schedulerProviderFactory,
      @Assisted GlobalKinesisConfiguration configuration) {
    this.schedulerProviderFactory = schedulerProviderFactory;
    this.configuration = configuration;
  }

  /** Starts the scheduler threads to begin polling records from the Kinesis streams configured */
  public void start() {
    // TODO Should loop on all the Streams and it should check if at last a
    // stream is available
    KinesisStreamItem kinesisStreamItem = configuration.getKinesisStreamItems().get(0);
    subscribe(kinesisStreamItem.getStreamName());
  }

  private void subscribe(String streamName) {
    this.streamName = streamName;
    this.kinesisScheduler = schedulerProviderFactory.create(streamName).get();
    Thread schedulerThread = new Thread(kinesisScheduler);
    schedulerThread.setDaemon(true);
    schedulerThread.start();
  }

  /** Stop the scheduler threads to end consuming records from the Kinesis streams */
  public void shutdown() {
    // TODO this is currently not plugged in
    Future<Boolean> gracefulShutdownFuture = kinesisScheduler.startGracefulShutdown();
    try {
      gracefulShutdownFuture.get(10L, TimeUnit.MILLISECONDS);
    } catch (Exception e) {

    }
  }
}
