package io.jenkins.plugins.aws.kinesisconsumer.listeners;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import jenkins.plugins.git.GitSCMSource;
import jenkins.scm.api.SCMEvent;
import org.junit.Test;

public class AWSKinesisSCMSourceEventTest {

  @Test
  public void shouldMatchProject() {
    String testProject = "testProject";
    assertTrue(
        new AWSKinesisSCMSourceEvent(
                SCMEvent.Type.UPDATED, "", testProject, "testStream", "SCMSourceId")
            .isMatch(new GitSCMSource("git@github.com:" + testProject + ".git")));
  }

  @Test
  public void shouldNotMatchProject() {
    String testProject = "testProject";
    assertFalse(
        new AWSKinesisSCMSourceEvent(
                SCMEvent.Type.UPDATED, "", "randomProject", "testStream", "SCMSourceId")
            .isMatch(new GitSCMSource("git" + "@github.com:" + testProject + ".git")));
  }
}
