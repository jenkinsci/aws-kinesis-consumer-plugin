package io.jenkins.plugins.aws.kinesisconsumer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class KinesisConsumerManagerTest extends BaseLocalStack {

  @Test
  public void shouldShutDownConsumers() throws InterruptedException {
    createStreamAndWait(STREAM_NAME);
    kinesisConsumerManager.start(globalKinesisConfiguration);

    waitForLeaseTable();

    KinesisConsumer kinesisConsumer = kinesisConsumerManager.getKinesisConsumer();
    assertTrue(kinesisConsumer.isStarted());

    // Shutdown Jenkins
    kinesisConsumerManager.onBeforeShutdown();

    assertFalse(kinesisConsumer.isStarted());
  }
}
