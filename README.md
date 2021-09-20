AWS Kinesis Consumer Plugin for Jenkins
=======================================================

[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/aws-kinesis-consumer.svg)](https://plugins.jenkins.io/aws-kinesis-consumer)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/aws-kinesis-consumer.svg)](https://plugins.jenkins.io/aws-kinesis-consumer)
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
      <groupId>io.jenkins.plugins</groupId>
      <artifactId>aws-kinesis-consumer</artifactId>
      <version>VERSION</version>
    </dependency>
  </dependencies>

</project>
```

Following is the Extension Point that will have to be implemented:

> io.jenkins.plugins.aws.kinesisconsumer.extensions.AWSKinesisStreamListener

Prepare a release
---

1. Update the `README` with the release notes. Use the following oneliner to
capture the changes since the last release:

```bash
> git log --pretty=oneline --abbrev-commit --no-merges
aws-kinesis-consumer-<latest_versions>..HEAD
```

2. Commit the `README` with the following message: `"Prepare v<new_version>
release notes"`. For example:

```bash
> git show  --pretty=format:"%s" -s  94398d33c4ca7f2010b01c8f6173a8c3cf79e0bf
Prepare v1.0.4 release notes
```
3. Perform the release (_NOTE: only the plugin maintainers have permissions to
publish the artifacts_):

```bash
> mvn release:clean release:prepare release:perform
```

The artifact will be available in the [Jenkins Maven repository](https://repo.jenkins-ci.org/artifactory/releases/io/jenkins/plugins/aws-kinesis-consumer/)

Plugin Releases
---

### v1.0.6 - Released - 20 September 2021

* `a28f012` Set Jenkins version to latest LTS 2.303.1

### v1.0.5 - Released - 17 September 2021

* `87f58c6` Bump up Jenkins version to 2.312

### v1.0.4 - Released - 15 September 2021

* `0fab7b6` Move stream filtering to client
* `6b3c9d1` Upgrade com.google.guava:guava
* `b29949b` Upgrade org.apache.commons:commons-compress

### v1.0.3 - Released - 14 September 2021

* `0e6bc81` Make consistent use of logger library
* `58ca196` Handle kinesis record processor phases
* `be03616` Restart consumers upon configuration change
* `67062a2` Introduce shutdownTimeoutMs configuration
* `ebc1f0c` Automatically start consumers when jenkins is ready
* `95deed1` Connect to multiple kinesis streams
* `c0867be` Consolidate AWS async client builder
* `088f0d6` Handle KinesisConsumer shutdown
* `abbd4f7` Make applicationName configurable

### v1.0.2 - Released - 13 September 2021

Initial version of the plugin to connect and consume records from AWS Kinesis

Issues
---
