AWS Kinesis Consumer Plugin for Jenkins
=======================================================

aws-kinesis-consumer is a Jenkins plugin to connect to [Kinesis](https://aws.amazon.com/kinesis/) and consume
records coming from specific streams.

This plugin has global configuration only, so any features for user are not provided.

Implement listener plugin
------------------------

This plugin provides an interface to listen application records coming from
Kinesis.

To implement listener in your plugin, the below dependencies need to be added in
your pom.xml:

```xml
<project>

  <dependencies>
    <dependency>
      <groupId>org.jenkins-ci.plugins</groupId>
      <artifactId>aws-kinesis-consumer</artifactId>
      <version>VERSION</version>
    </dependency>
  </dependencies>

</project>
```

Following is the Extension Point that will have to be implemented:

> io.jenkins.plugins.aws.kinesisconsumer.extensions.AWSKinesisStreamListener

Plugin Releases
---

### v0.1.0 - Released - DD MM YYYY

Initial version of the plugin to connect to AWS Kinesis

Issues
---