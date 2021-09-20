package io.jenkins.plugins.aws.kinesisconsumer.listeners;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.flogger.FluentLogger;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import hudson.Extension;
import hudson.security.ACL;
import hudson.security.ACLContext;
import io.jenkins.plugins.aws.kinesisconsumer.GlobalKinesisConfiguration;
import io.jenkins.plugins.aws.kinesisconsumer.KinesisStreamItem;
import io.jenkins.plugins.aws.kinesisconsumer.extensions.AWSKinesisStreamListener;
import java.util.List;
import java.util.Optional;
import jenkins.model.Jenkins;
import jenkins.plugins.git.AbstractGitSCMSource;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceOwner;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.jetbrains.annotations.NotNull;

/**
 * AWSKinesisStreamListener implementation listening to events coming from AWS Kinesis streams.
 *
 * <p>If as stream has been configured to trigger an SCM build, the listener will extract the
 * project name from the payload of the event and will lookup for the SCM Sources to trigger a scan
 * for.
 *
 * @author Fabio Ponciroli
 */
@Extension
public class AWSKinesisStreamListenerImpl extends AWSKinesisStreamListener {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  @Override
  public void onReceive(String streamName, String jsonPayload) {
    getProjectFromEvent(streamName, jsonPayload)
        .ifPresent(
            projectFromEvent -> {
              String username = getJenkinsInstance().getAuthentication2().getName();

              logger.atInfo().log(
                  String.format(
                      "SCM action invoked by user '%s' for project: %s",
                      username, projectFromEvent));
              try (ACLContext acl = ACL.as(ACL.SYSTEM)) {
                List<WorkflowMultiBranchProject> jenkinsItems =
                    getJenkinsInstance().getAllItems(WorkflowMultiBranchProject.class);
                logger.atInfo().log("Scanning %d Jenkins items", jenkinsItems.size());
                for (SCMSourceOwner scmJob : jenkinsItems) {
                  logger.atInfo().log("Scanning job " + scmJob);
                  List<SCMSource> scmSources = scmJob.getSCMSources();
                  for (SCMSource scmSource : scmSources) {
                    if (triggerSCMBuildForSource(scmSource, projectFromEvent)) {
                      logger.atInfo().log(
                          "Triggering build for project %s, SCM id %s",
                          projectFromEvent, scmSource.getId());
                      scmJob.onSCMSourceUpdated(scmSource);
                    } else {
                      logger.atFine().log(
                          "No build to trigger for project %s, SCM id %s",
                          projectFromEvent, scmSource.getId());
                    }
                  }
                }
              }
            });
  }

  @VisibleForTesting
  public boolean triggerSCMBuildForSource(SCMSource scmSource, String projectFromEvent) {
    return scmSource instanceof AbstractGitSCMSource
        && sourceMatchProject(scmSource, projectFromEvent);
  }

  private boolean sourceMatchProject(@NotNull SCMSource source, String projectFromEvent) {
    String remote = ((AbstractGitSCMSource) source).getRemote();
    return remote.replaceFirst("\\.git$", "").endsWith(projectFromEvent);
  }

  @VisibleForTesting
  public Optional<String> getProjectFromEvent(String streamName, String jsonPayload) {
    return getProjectField(streamName)
        .flatMap(
            projectField -> {
              try {
                return Optional.of(JsonPath.read(jsonPayload, projectField));
              } catch (PathNotFoundException pnfe) {
                logger.atSevere().withCause(pnfe).log("Invalid Json Path expression");
              }
              return Optional.empty();
            });
  }

  private Optional<String> getProjectField(String streamName) {
    GlobalKinesisConfiguration globalKinesisConfiguration = GlobalKinesisConfiguration.get();

    KinesisStreamItem kinesisStreamItem =
        globalKinesisConfiguration.getKinesisStreamItemsForStream(streamName);

    if (!kinesisStreamItem.getTriggerSCMBuild()) {
      return Optional.empty();
    }

    return Optional.of(kinesisStreamItem.getProjectNameJsonPath());
  }

  private Jenkins getJenkinsInstance() {
    Jenkins jenkins = Jenkins.getInstanceOrNull();
    if (jenkins != null) {
      return jenkins;
    }
    throw new IllegalStateException("Jenkins is not started or is stopped");
  }
}
