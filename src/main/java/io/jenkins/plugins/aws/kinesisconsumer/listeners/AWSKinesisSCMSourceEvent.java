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
  private final String project;
  private final String SCMSourceId;

  public AWSKinesisSCMSourceEvent(
      @NonNull Type type,
      @NonNull String payload,
      @CheckForNull String project,
      String streamName,
      String SCMSourceId) {
    super(type, payload, "aws-kinesis" + '|' + streamName);
    this.project = project;
    this.SCMSourceId = SCMSourceId;
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
    return SCMSourceId;
  }
}
