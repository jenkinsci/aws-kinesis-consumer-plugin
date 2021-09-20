package io.jenkins.plugins.aws.kinesisconsumer.listeners;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import jenkins.plugins.git.AbstractGitSCMSource;
import jenkins.scm.api.SCMNavigator;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceEvent;
import org.jetbrains.annotations.NotNull;

/**
 * SCM Source event coming from AWS Kinesis
 *
 * @author Fabio Ponciroli
 */
public class AWSKinesisSCMSourceEvent extends SCMSourceEvent<String> {
  private static final String SOURCE_NAME = "scm-trigger-aws-kinesis-consumer";
  private final String project;

  public AWSKinesisSCMSourceEvent(
      @NonNull Type type,
      @NonNull String payload,
      @CheckForNull String project,
      String streamName) {
    super(type, payload, SOURCE_NAME + '|' + project + '|' + streamName);
    this.project = project;
  }

  @Override
  public boolean isMatch(@NotNull SCMNavigator navigator) {
    return false;
  }

  @Override
  public boolean isMatch(@NotNull SCMSource source) {
    String remote = ((AbstractGitSCMSource) source).getRemote();
    return remote.replaceFirst("\\.git$", "").endsWith(project);
  }

  @NotNull
  @Override
  public String getSourceName() {
    return SOURCE_NAME;
  }
}
