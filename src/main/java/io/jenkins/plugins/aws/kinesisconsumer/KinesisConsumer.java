package io.jenkins.plugins.aws.kinesisconsumer;

import com.google.common.flogger.FluentLogger;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import software.amazon.kinesis.coordinator.Scheduler;

public class KinesisConsumer {
  interface Factory {
    KinesisConsumer create(GlobalKinesisConfiguration configuration);
  }

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private String streamName;
  private Scheduler kinesisScheduler;
  private final SchedulerProvider schedulerProvider;
  private GlobalKinesisConfiguration configuration;

  @AssistedInject
  KinesisConsumer(
      SchedulerProvider schedulerProvider, @Assisted GlobalKinesisConfiguration configuration) {
    this.schedulerProvider = schedulerProvider;
    this.configuration = configuration;
  }

  public void start() {
    // TODO Should loop on all the Streams and it should check if at last a
    // stream is available
    KinesisStreamItem kinesisStreamItem = configuration.getKinesisStreamItems().get(0);
    subscribe(kinesisStreamItem.getStreamName());
  }

  public void subscribe(String streamName) {
    this.streamName = streamName;
    this.kinesisScheduler = schedulerProvider.forStream(streamName).get();
    Thread schedulerThread = new Thread(kinesisScheduler);
    schedulerThread.setDaemon(true);
    schedulerThread.start();
  }

  public void shutdown() {
    // TODO this is currently not plugged in
    Future<Boolean> gracefulShutdownFuture = kinesisScheduler.startGracefulShutdown();
    try {
      gracefulShutdownFuture.get(10L, TimeUnit.MILLISECONDS);
    } catch (Exception e) {

    }
  }
}
