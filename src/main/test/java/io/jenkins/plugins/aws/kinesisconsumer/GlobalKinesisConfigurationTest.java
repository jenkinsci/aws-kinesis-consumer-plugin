package io.jenkins.plugins.aws.kinesisconsumer;

import com.google.common.collect.ImmutableList;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.For;
import org.jvnet.hudson.test.JenkinsRule;

@For(GlobalKinesisConfiguration.class)
public class GlobalKinesisConfigurationTest {

  @Rule public JenkinsRule j = new JenkinsRule();

  @Test
  public void configRoundtrip() {
    GlobalKinesisConfiguration c = GlobalKinesisConfiguration.get();
    c.setLocalEndpoint("http://localhost:4566");
    c.setRegion("eu-east-1");
    c.setKinesisConsumerEnabled(true);
    c.setKinesisStreamItems(ImmutableList.of(new KinesisStreamItem("stream_foo")));
    c.save();
    c.load();
  }
}
