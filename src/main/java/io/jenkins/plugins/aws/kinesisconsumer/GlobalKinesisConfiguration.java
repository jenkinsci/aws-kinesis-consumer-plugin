package io.jenkins.plugins.aws.kinesisconsumer;

import hudson.Extension;
import hudson.ExtensionList;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Class representing the Global Kinesis configuration
 *
 * @author Fabio Ponciroli
 */

@Extension
@Symbol("aws-kinesis-consumer")
public class GlobalKinesisConfiguration extends GlobalConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalKinesisConfiguration.class);
    private boolean kinesisConsumerEnabled;
    private String region;
    private List <KinesisStreamItem> kinesisStreamItems;
    private String localEndpoint;

    /**
     * Set AWS Region when loading the global configuration page
     * @param regionString
     *          AWS Region to connect to. This parameter is optional.
     *          If set it will override any Region set in the Region Provider Chain.
     */
    @DataBoundSetter
    public void setRegion(String regionString) {
        this.region = regionString;
    }

    /**
     * Set the enabled/disabled status of the plugin when loading the global configuration page
     * @param kinesisConsumerEnabled
     *          enabled/disabled status
     */
    @DataBoundSetter
    public void setKinesisConsumerEnabled(boolean kinesisConsumerEnabled) {
        this.kinesisConsumerEnabled = kinesisConsumerEnabled;
    }

    /**
     * Set KinesisStreamItem values when loading the global configuration page
     * @param kinesisStreamItems
     *          KinesisStreamItem to set
     */
    @DataBoundSetter
    public void setKinesisStreamItems(List <KinesisStreamItem> kinesisStreamItems) {
        this.kinesisStreamItems = kinesisStreamItems;
    }

    /**
     * Set an optional endpoint to point to a local kinesis stack rather than
     * the AWS service. Useful for development.
     * @param localEndpoint the local endpoint URL, i.e. http://localhost:4566
     */
    @DataBoundSetter
    public void setLocalEndpoint(String localEndpoint) {
        this.localEndpoint = localEndpoint;
    }

    public GlobalKinesisConfiguration() { load(); }

    /**
     * Get the GlobalKinesisConfiguration
     *
     * @return the global Kinesis configuration
     */
    public static GlobalKinesisConfiguration get() {
        return ExtensionList.lookupSingleton(GlobalKinesisConfiguration.class);
    }

    /**
     * Indicates if the plugin is enabled or not
     *
     * @return a boolean indicating the status of the plugin
     */
    public boolean isKinesisConsumerEnabled() {
        return kinesisConsumerEnabled;
    }

    /**
     * Get the AWS Region to connect to from the configuration page
     *
     * @return AWS Region to connect to
     */
    public String getRegion() {
        return region;
    }

    public List <KinesisStreamItem> getKinesisStreamItems() {
        return kinesisStreamItems;
    }

    /**
     * Get the local endpoint to consume from rather than the AWS service
     *
     * @return The local endpoint, or null, when not defined.
     */
    public String getLocalEndpoint() {
        return localEndpoint;
    }

    /**
     *
     * @param req
     *          <code>StaplerRequest</code> submitted when saving the
     *          configuration page
     * @param json
     *          JSON containing the configuration parameters set
     * @return <code>true<code/> if the configuration was correctly saved,
     *          <code>false</code> otherwise
     */
    @Override
    public boolean configure(StaplerRequest req, JSONObject json) {
        if (kinesisStreamItems != null) {
            kinesisStreamItems.clear();
        }
        req.bindJSON(this, json);
        save();
        return true;
    }
}