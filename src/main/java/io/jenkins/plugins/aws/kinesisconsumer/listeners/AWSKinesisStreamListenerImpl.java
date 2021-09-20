package io.jenkins.plugins.aws.kinesisconsumer.listeners;

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
import jenkins.scm.api.SCMEvent;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceEvent;
import jenkins.scm.api.SCMSourceOwner;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;

/**
 * AWSKinesisStreamListener implementation listening to events coming
 * from AWS Kinesis streams.
 *
 * If as stream has been configured to trigger an SCM build, the listener will
 * extract the project name from the payload of the event and will lookup for
 * the SCM Sources to trigger a scan for.
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
                      "GerritWebHook " + "invoked by user '%s' for project: %s",
                      username, projectFromEvent));
              try (ACLContext acl = ACL.as(ACL.SYSTEM)) {
                List<WorkflowMultiBranchProject> jenkinsItems =
                    getJenkinsInstance().getAllItems(WorkflowMultiBranchProject.class);
                logger.atInfo().log("Scanning %d Jenkins items",
                 jenkinsItems.size());
                for (SCMSourceOwner scmJob : jenkinsItems) {
                  logger.atInfo().log("Scanning job " + scmJob);
                  List<SCMSource> scmSources = scmJob.getSCMSources();
                  for (SCMSource scmSource : scmSources) {
                    AWSKinesisSCMSourceEvent event =
                        new AWSKinesisSCMSourceEvent(
                            SCMEvent.Type.UPDATED, "", projectFromEvent, streamName);
                    filterEventsForSource(scmSource, event)
                        .ifPresent(
                            e -> {
                              logger.atInfo().log(
                                  "Triggering build" + " for project %s, SCM " +
                                   "id %s",
                                  projectFromEvent,
                                  scmSource.getId());
                              SCMSourceEvent.fireNow(e);
                            });
                  }
                }
              }
            });
  }

  private Optional<AWSKinesisSCMSourceEvent> filterEventsForSource(
      SCMSource scmSource, AWSKinesisSCMSourceEvent event) {
    if (scmSource instanceof AbstractGitSCMSource && event.isMatch(scmSource)) {
      return Optional.of(event);
    }
    return Optional.empty();
  }

  private Optional<String> getProjectFromEvent(String streamName, String jsonPayload) {
    return getProjectField(streamName)
        .flatMap(
            projectField -> {
              try {
                return Optional.of(JsonPath.read(jsonPayload,projectField));
              } catch (PathNotFoundException pnfe) {
                  logger.atSevere().withCause(pnfe).log("Invalid Json Path");
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

    return Optional.of(kinesisStreamItem.getProjectNameField());
  }

  private Jenkins getJenkinsInstance() {
    Jenkins jenkins = Jenkins.getInstanceOrNull();
    if (jenkins != null) {
      return jenkins;
    }
    throw new IllegalStateException("Jenkins is not started or is stopped");
  }
}
