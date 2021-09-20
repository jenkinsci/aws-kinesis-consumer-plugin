package io.jenkins.plugins.aws.kinesisconsumer.listeners;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.flogger.FluentLogger;
import hudson.Extension;
import hudson.security.ACL;
import hudson.security.ACLContext;
import io.jenkins.plugins.aws.kinesisconsumer.extensions.AWSKinesisStreamListener;
import jenkins.model.Jenkins;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceOwner;
import org.acegisecurity.Authentication;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Extension
public class AWSKinesisStreamListenerImpl extends AWSKinesisStreamListener {

    private static final FluentLogger logger = FluentLogger.forEnclosingClass();

    @Override
    public void onReceive(String streamName, byte[] bytes) {
        //TODO Need to check if triggering the SCM has been enableed in the
        // config
        getGerritProjectFromEvent(bytes)
                .ifPresent(
                    projectEvent -> {
                        String username = "anonymous";
                        Authentication authentication =
                         getJenkinsInstance().getAuthentication();
                        if (authentication != null) {
                            username = authentication.getName();
                        }

                        logger.atInfo().log(String.format("GerritWebHook " +
                         "invoked by user '%s' for event: %s", username,
                          projectEvent));
                        try (ACLContext acl = ACL.as(ACL.SYSTEM)) {
                            List <WorkflowMultiBranchProject> jenkinsItems =
                                    getJenkinsInstance().getAllItems(WorkflowMultiBranchProject.class);
                            logger.atInfo().log("Scanning {} Jenkins items",
                             jenkinsItems.size());
                            for (SCMSourceOwner scmJob : jenkinsItems) {
                                logger.atInfo().log("Scanning job " + scmJob);
                                List<SCMSource> scmSources = scmJob.getSCMSources();
                                for (SCMSource scmSource : scmSources) {
                                    if(getGerritProjectFromEvent(bytes).equals(getProjectFromRemote(scmSource))) {
                                        scmJob.onSCMSourceUpdated(scmSource);
                                    }
                                }
                            }
                        }
                    });
    }

    String getProjectFromRemote(SCMSource scmSource) {
        //TODO Extract project from scmSource
        return "theRemote";
    }

    Optional <String> getGerritProjectFromEvent(byte[] bytes) {
        ObjectMapper jsonMapper = new ObjectMapper();
        Map <String, String> stringStringMap;
        try {
            stringStringMap =
                    jsonMapper.readValue(new String(bytes), new TypeReference <Map<String, String>>() {});

            return Optional.of(stringStringMap.get("project"));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Jenkins getJenkinsInstance() {
        Jenkins jenkins = Jenkins.getInstanceOrNull();
        if (jenkins != null) {
            return jenkins;
        }
        throw new IllegalStateException("Jenkins is not started or is stopped");
    }
}