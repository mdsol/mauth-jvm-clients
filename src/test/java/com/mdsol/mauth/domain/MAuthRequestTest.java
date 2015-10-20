package com.mdsol.mauth.domain;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.apache.http.client.methods.HttpPost;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class MAuthRequestTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static final UUID CLIENT_APP_UUID =
      UUID.fromString("92a1869e-c80d-4f06-8775-6c4ebb0758e0");
  private static final String CLIENT_REQUEST_SIGNATURE =
      "fFQzIOo4S1MxxmEDB9v7v0IYNytnS3I5aHNeJfEfFe1v1gTE/cH36BfLG"
          + "/zpOHX7kPUDUnmVZK1MuqdenLDmY6q2h8BR/1yA7n/dupQfnVZIeaB99GIO"
          + "FJaHT6P6gXHiMTFxgX3Oo/rj97jfDUxaunxnlqnfhHccPxbhiqfcVgHahw6hiX"
          + "x9sAt/iG/Yu7lzZbWx4SWkDyAGvk1I5rpbxcOWLPYad8qpuCFaHPMQJTTze8G02hd5"
          + "ocZDzQS73MOfUqPLfvv7aLRvBS078cqs2uAip84n8bM4qG60gMfoC9kUluza7i9poyFqq"
          + "IdsCnS5RQuyNcsixneX2X3CNt3yOw==";
  private static final String CLIENT_REQUEST_METHOD = HttpPost.METHOD_NAME;
  private static final String CLIENT_REQUEST_PATH = "/resource/path";
  private static final byte[] CLIENT_REQUEST_PAYLOAD =
      "message here".getBytes(StandardCharsets.UTF_8);
  private static final String CLIENT_REQUEST_TIME = "1444672122";

  @Test
  public void shouldNotAllowToCreateRequestWithoutAppUUID() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Application UUID cannot be null or empty.");

    MAuthRequest.Builder.get().withHttpMethod(CLIENT_REQUEST_METHOD)
        .withMessagePayload(CLIENT_REQUEST_PAYLOAD).withRequestSignature(CLIENT_REQUEST_SIGNATURE)
        .withRequestTime(CLIENT_REQUEST_TIME).withResourcePath(CLIENT_REQUEST_PATH).build();
  }

  @Test
  public void shouldNotAllowToCreateRequestWithoutHttpMethod() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Http method cannot be null or empty.");

    MAuthRequest.Builder.get().withAppUUID(CLIENT_APP_UUID)
        .withMessagePayload(CLIENT_REQUEST_PAYLOAD).withRequestSignature(CLIENT_REQUEST_SIGNATURE)
        .withRequestTime(CLIENT_REQUEST_TIME).withResourcePath(CLIENT_REQUEST_PATH).build();
  }

  @Test
  public void shouldNotAllowToCreateRequestWithoutMessagePayload() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Message payload cannot be null or empty.");

    MAuthRequest.Builder.get().withHttpMethod(CLIENT_REQUEST_METHOD).withAppUUID(CLIENT_APP_UUID)
        .withRequestSignature(CLIENT_REQUEST_SIGNATURE).withRequestTime(CLIENT_REQUEST_TIME)
        .withResourcePath(CLIENT_REQUEST_PATH).build();
  }

  @Test
  public void shouldNotAllowToCreateRequestWithEmptyMessagePayload() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Message payload cannot be null or empty.");

    MAuthRequest.Builder.get().withHttpMethod(CLIENT_REQUEST_METHOD).withAppUUID(CLIENT_APP_UUID)
        .withRequestSignature(CLIENT_REQUEST_SIGNATURE).withRequestTime(CLIENT_REQUEST_TIME)
        .withResourcePath(CLIENT_REQUEST_PATH).withMessagePayload(new byte[] {}).build();
  }

  @Test
  public void shouldNotAllowToCreateRequestWithoutRequestTime() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Request time cannot be null or empty.");

    MAuthRequest.Builder.get().withHttpMethod(CLIENT_REQUEST_METHOD).withAppUUID(CLIENT_APP_UUID)
        .withRequestSignature(CLIENT_REQUEST_SIGNATURE).withMessagePayload(CLIENT_REQUEST_PAYLOAD)
        .withResourcePath(CLIENT_REQUEST_PATH).build();
  }

  @Test
  public void shouldNotAllowToCreateRequestWithInvalidFormatOfRequestTime() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Request time must express the epoch time.");

    MAuthRequest.Builder.get().withHttpMethod(CLIENT_REQUEST_METHOD).withAppUUID(CLIENT_APP_UUID)
        .withRequestSignature(CLIENT_REQUEST_SIGNATURE).withMessagePayload(CLIENT_REQUEST_PAYLOAD)
        .withResourcePath(CLIENT_REQUEST_PATH).withRequestTime("10/10/16 13:21:58").build();
  }

  @Test
  public void shouldNotAllowToCreateRequestWithoutRequestPath() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Resource path cannot be null or empty.");

    MAuthRequest.Builder.get().withHttpMethod(CLIENT_REQUEST_METHOD).withAppUUID(CLIENT_APP_UUID)
        .withRequestSignature(CLIENT_REQUEST_SIGNATURE).withRequestTime(CLIENT_REQUEST_TIME)
        .withMessagePayload(CLIENT_REQUEST_PAYLOAD).build();
  }

  @Test
  public void shouldNotAllowToCreateRequestWithoutRequestSignature() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Request signature cannot be null or empty.");

    MAuthRequest.Builder.get().withHttpMethod(CLIENT_REQUEST_METHOD).withAppUUID(CLIENT_APP_UUID)
        .withResourcePath(CLIENT_REQUEST_PATH).withRequestTime(CLIENT_REQUEST_TIME)
        .withMessagePayload(CLIENT_REQUEST_PAYLOAD).build();
  }

  @Test
  public void shouldCorrectlyCreateMAuthRequest() {
    MAuthRequest request = MAuthRequest.Builder.get().withAppUUID(CLIENT_APP_UUID)
        .withHttpMethod(CLIENT_REQUEST_METHOD).withMessagePayload(CLIENT_REQUEST_PAYLOAD)
        .withRequestSignature(CLIENT_REQUEST_SIGNATURE).withRequestTime(CLIENT_REQUEST_TIME)
        .withResourcePath(CLIENT_REQUEST_PATH).build();

    assertThat(request.getAppUUID(), equalTo(CLIENT_APP_UUID));
    assertThat(request.getHttpMethod(), equalTo(CLIENT_REQUEST_METHOD));
    assertThat(request.getResourcePath(), equalTo(CLIENT_REQUEST_PATH));
    assertThat(request.getRequestTime(), equalTo(Long.parseLong(CLIENT_REQUEST_TIME)));
    assertThat(request.getMessagePayload(), equalTo(CLIENT_REQUEST_PAYLOAD));
  }

}
