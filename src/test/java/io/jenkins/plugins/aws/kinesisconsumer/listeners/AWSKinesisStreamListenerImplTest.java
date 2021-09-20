package io.jenkins.plugins.aws.kinesisconsumer.listeners;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.jenkins.plugins.aws.kinesisconsumer.GlobalKinesisConfiguration;
import io.jenkins.plugins.aws.kinesisconsumer.KinesisStreamItem;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import jenkins.branch.BranchSource;
import jenkins.plugins.git.GitSCMSource;
import jenkins.scm.api.SCMSourceEvent;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

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
  public void shouldNotGetProjectFromEventIfSCMTriggerIsNotEnabled() {
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

  @Test
  public void shouldTriggerSCMBuild() throws Exception {
    String streamName = "testStream";
    String projectName = "testProject";
    setWorkflowMultiBranchProject(projectName);
    setGlobalConfiguration(streamName, "$.projectField", true);

    try (MockedStatic<SCMSourceEvent> mockedSCMSourceEvent =
        Mockito.mockStatic(SCMSourceEvent.class)) {
      mockedSCMSourceEvent.when(() -> SCMSourceEvent.fireNow(Mockito.any())).thenCallRealMethod();

      new AWSKinesisStreamListenerImpl()
          .onReceive(streamName, "{\"projectField\":\"" + projectName + "\"}");
      j.waitUntilNoActivity();

      mockedSCMSourceEvent.verify(() -> SCMSourceEvent.fireNow(Mockito.any()));
    }
  }

  @Test
  public void shouldNotTriggerSCMBuild() throws Exception {
    String streamName = "testStream";
    String projectName = "testProject";
    setWorkflowMultiBranchProject(projectName);
    setGlobalConfiguration(streamName, "$.randomProject", true);

    try (MockedStatic<SCMSourceEvent> mockedSCMSourceEvent =
        Mockito.mockStatic(SCMSourceEvent.class)) {
      mockedSCMSourceEvent.when(() -> SCMSourceEvent.fireNow(Mockito.any())).thenCallRealMethod();

      new AWSKinesisStreamListenerImpl()
          .onReceive(streamName, "{\"projectField\":\"" + projectName + "\"}");
      j.waitUntilNoActivity();

      mockedSCMSourceEvent.verifyNoInteractions();
    }
  }

  private void setWorkflowMultiBranchProject(String projectName) throws IOException {
    WorkflowMultiBranchProject mp =
        j.jenkins.createProject(WorkflowMultiBranchProject.class, projectName);
    mp.getSourcesList().add(new BranchSource(getGitSCMSource(projectName)));
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

  private GitSCMSource getGitSCMSource(String projectName) {
    GitSCMSource mockSource = mock(GitSCMSource.class);
    when(mockSource.getRemote()).thenReturn("git@github.com:" + projectName + ".git");
    return mockSource;
  }
}
