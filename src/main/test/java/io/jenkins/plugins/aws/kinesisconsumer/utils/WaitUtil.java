package io.jenkins.plugins.aws.kinesisconsumer.utils;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.google.common.base.Stopwatch;
import java.time.Duration;
import java.util.function.Supplier;

public class WaitUtil {
  public static void waitUntil(Supplier<Boolean> waitCondition, Duration timeout)
      throws InterruptedException {
    Stopwatch stopwatch = Stopwatch.createStarted();
    while (!waitCondition.get()) {
      if (stopwatch.elapsed().compareTo(timeout) > 0) {
        throw new InterruptedException();
      }
      MILLISECONDS.sleep(50);
    }
  }
}
