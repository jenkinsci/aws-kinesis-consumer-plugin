package io.jenkins.plugins.aws.kinesisconsumer;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableList;
import hudson.util.FormValidation;
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

  @Test
  public void shouldCheckInvalidLocalEndpoint() {
    GlobalKinesisConfiguration c = GlobalKinesisConfiguration.get();

    FormValidation result = c.doCheckLocalEndpoint("not_a_url");

    assertEquals(result.kind, FormValidation.Kind.ERROR);
    assertEquals(
        result.getMessage(), FormValidation.error("'not_a_url' is not a valid URL").getMessage());
  }

  @Test
  public void shouldCheckValidLocalEndpoint() {
    GlobalKinesisConfiguration c = GlobalKinesisConfiguration.get();

    assertEquals(c.doCheckLocalEndpoint("http://localhost:4566").kind, FormValidation.Kind.OK);
  }
}
