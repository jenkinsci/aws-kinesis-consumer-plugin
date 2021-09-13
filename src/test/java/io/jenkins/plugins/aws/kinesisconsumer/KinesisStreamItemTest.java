package io.jenkins.plugins.aws.kinesisconsumer;

import static org.junit.Assert.assertEquals;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.KINESIS;

import hudson.util.FormValidation;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.test.For;

@For(KinesisStreamItem.class)
public class KinesisStreamItemTest extends BaseLocalStack {
  private KinesisStreamItem.DescriptorImpl objectUnderTest;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    objectUnderTest = new KinesisStreamItem.DescriptorImpl();
  }

  @Test
  public void shouldFailWhenStreamDoesNotExist() {
    FormValidation result =
        objectUnderTest.doTestConnection(
            localstack.getRegion(),
            localstack.getEndpointOverride(KINESIS).toASCIIString(),
            "notExisting");
    assertEquals(
        String.format("Form Validation error was: %s", result.renderHtml()),
        FormValidation.Kind.ERROR,
        result.kind);
  }

  @Test
  public void shouldSucceedWhenStreamDoesExist() throws InterruptedException {
    String streamName = "foobar";
    createStreamAndWait(streamName);
    FormValidation result =
        objectUnderTest.doTestConnection(
            localstack.getRegion(),
            localstack.getEndpointOverride(KINESIS).toASCIIString(),
            streamName);
    assertEquals(
        String.format("Form Validation error was: %s", result.renderHtml()),
        FormValidation.Kind.OK,
        result.kind);
  }
}
