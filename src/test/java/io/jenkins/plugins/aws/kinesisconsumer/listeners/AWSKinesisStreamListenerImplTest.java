package io.jenkins.plugins.aws.kinesisconsumer.listeners;

import static org.junit.Assert.assertEquals;

import io.jenkins.plugins.aws.kinesisconsumer.GlobalKinesisConfiguration;
import io.jenkins.plugins.aws.kinesisconsumer.KinesisStreamItem;
import java.util.Collections;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class AWSKinesisStreamListenerImplTest {
  @Rule public JenkinsRule j = new JenkinsRule();

  @Test
  public void shouldGetProjectFromEventIfSCMTriggerIsEnabled() {
    String streamName = "testStream";
    String projectName = "testProject";
    setGlobalConfiguration(streamName, "$.projectField", true);

    assertEquals(
        "Get expected project",
        Optional.of(projectName),
        new AWSKinesisStreamListenerImpl()
            .getProjectFromEvent(streamName, "{\"projectField\":\"" + projectName + "\"}"));
  }

  @Test
  public void shouldNotGetProjectFromEventIfSCMTriggerIsEnabled() {
    String streamName = "testStream";
    String projectName = "testProject";
    setGlobalConfiguration(streamName, "$.projectField", false);

    assertEquals(
        "Get expected project",
        Optional.empty(),
        new AWSKinesisStreamListenerImpl()
            .getProjectFromEvent(streamName, "{\"projectField\":\"" + projectName + "\"}"));
  }

  @Test
  public void shouldNotGetProjectFromEventIfJsonPathDontMatch() {
    String streamName = "testStream";
    String projectName = "testProject";
    setGlobalConfiguration(streamName, "$.randomField", true);

    assertEquals(
        "Get expected project",
        Optional.empty(),
        new AWSKinesisStreamListenerImpl()
            .getProjectFromEvent(streamName, "{\"projectField\":\"" + projectName + "\"}"));
  }

  private void setGlobalConfiguration(
      String streamName, String projectJsonPath, Boolean triggerSCMBuild) {
    GlobalKinesisConfiguration c = GlobalKinesisConfiguration.get();
    KinesisStreamItem kinesisStreamItem =
        new KinesisStreamItem(streamName, "TRIM_HORIZON", projectJsonPath, triggerSCMBuild);
    c.setKinesisStreamItems(Collections.singletonList(kinesisStreamItem));
    c.save();
    c.load();
  }
}
