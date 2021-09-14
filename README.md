AWS Kinesis Consumer Plugin for Jenkins
=======================================================

[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/aws-kinesis-consumer.svg)](https://plugins.jenkins.io/gerrit-code-review)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/gerrit-code-review.svg?color=blue)](https://plugins.jenkins.io/gerrit-code-review)
[![Build Status](https://ci.jenkins.io/buildStatus/icon?job=Plugins/aws-kinesis-consumer-plugin/master)](https://ci.jenkins.io/job/Plugins/job/aws-kinesis-consumer-plugin/job/master/)

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

### v1.0.2 - Released - 13 September 2021

Initial version of the plugin to connect and consume records from AWS Kinesis

Issues
---
