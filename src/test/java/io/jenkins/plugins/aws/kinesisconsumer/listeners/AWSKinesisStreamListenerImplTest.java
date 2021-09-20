package io.jenkins.plugins.aws.kinesisconsumer.listeners;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import hudson.model.Result;
import io.jenkins.plugins.aws.kinesisconsumer.GlobalKinesisConfiguration;
import io.jenkins.plugins.aws.kinesisconsumer.KinesisStreamItem;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import jenkins.branch.BranchSource;
import jenkins.plugins.git.GitSCMSource;
import jenkins.scm.api.SCMSource;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class AWSKinesisStreamListenerImplTest {
  @Rule public JenkinsRule j = new JenkinsRule();

  @Test
  public void shouldTriggerSCMBuildForSource() {
    String projectName = "testProject";
    GitSCMSource scmSource = getGitSCMSource(projectName);

    assertTrue(new AWSKinesisStreamListenerImpl().triggerSCMBuildForSource(scmSource, projectName));
  }

  @Test
  public void shouldNotTriggerSCMBuildForSource() {
    GitSCMSource scmSource = getGitSCMSource("testProject");

    assertFalse(
        new AWSKinesisStreamListenerImpl()
            .triggerSCMBuildForSource(scmSource, "nonMatchingProject"));
  }

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
    GitSCMSource gitSCMSource = getGitSCMSource(projectName);
    WorkflowMultiBranchProject mp = setWorkflowMultiBranchProject(projectName, gitSCMSource);
    setGlobalConfiguration(streamName, "$.projectField", true);

    new AWSKinesisStreamListenerImpl()
        .onReceive(streamName, "{\"projectField\":\"" + projectName + "\"}");
    j.waitUntilNoActivity();

    assertEquals(Result.SUCCESS, mp.getIndexing().getResult());
  }

  @Test
  public void shouldNotTriggerSCMBuild() throws Exception {
    String streamName = "testStream";
    String projectName = "testProject";
    GitSCMSource gitSCMSource = getGitSCMSource(projectName);
    WorkflowMultiBranchProject mp = setWorkflowMultiBranchProject(projectName, gitSCMSource);
    setGlobalConfiguration(streamName, "$.randomProject", true);

    new AWSKinesisStreamListenerImpl()
        .onReceive(streamName, "{\"projectField\":\"" + projectName + "\"}");
    j.waitUntilNoActivity();

    assertNull(mp.getIndexing().getResult());
  }

  private WorkflowMultiBranchProject setWorkflowMultiBranchProject(
      String projectName, SCMSource scmSource) throws IOException {
    WorkflowMultiBranchProject mp =
        j.jenkins.createProject(WorkflowMultiBranchProject.class, projectName);
    mp.getSourcesList().add(new BranchSource(scmSource));
    return mp;
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
