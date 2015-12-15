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

  private static final String CLIENT_APP_UUID = "92a1869e-c80d-4f06-8775-6c4ebb0758e0";
  private static final String CLIENT_REQUEST_SIGNATURE =
      "fFQzIOo4S1MxxmEDB9v7v0IYNytnS3I5aHNeJfEfFe1v1gTE/cH36BfLG"
          + "/zpOHX7kPUDUnmVZK1MuqdenLDmY6q2h8BR/1yA7n/dupQfnVZIeaB99GIO"
          + "FJaHT6P6gXHiMTFxgX3Oo/rj97jfDUxaunxnlqnfhHccPxbhiqfcVgHahw6hiX"
          + "x9sAt/iG/Yu7lzZbWx4SWkDyAGvk1I5rpbxcOWLPYad8qpuCFaHPMQJTTze8G02hd5"
          + "ocZDzQS73MOfUqPLfvv7aLRvBS078cqs2uAip84n8bM4qG60gMfoC9kUluza7i9poyFqq"
          + "IdsCnS5RQuyNcsixneX2X3CNt3yOw==";
  private static final String CLIENT_REQUEST_AUTHENTICATION_HEADER =
      "MWS " + CLIENT_APP_UUID + ":" + CLIENT_REQUEST_SIGNATURE;
  private static final String CLIENT_REQUEST_TIME_HEADER = "1444672122";
  private static final String CLIENT_REQUEST_METHOD = HttpPost.METHOD_NAME;
  private static final String CLIENT_REQUEST_PATH = "/resource/path";
  private static final byte[] CLIENT_REQUEST_PAYLOAD =
      "message here".getBytes(StandardCharsets.UTF_8);

  @Test
  public void shouldNotAllowToCreateRequestWithoutRequestAuthenticationHeader() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Authentication header value cannot be null or empty.");

    MAuthRequest.Builder.get().withHttpMethod(CLIENT_REQUEST_METHOD)
        .withMessagePayload(CLIENT_REQUEST_PAYLOAD).withTimeHeaderValue(CLIENT_REQUEST_TIME_HEADER)
        .withResourcePath(CLIENT_REQUEST_PATH).build();
  }

  @Test
  public void shouldNotAllowToCreateRequestWithoutHttpMethod() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Http method cannot be null or empty.");

    MAuthRequest.Builder.get().withAuthenticationHeaderValue(CLIENT_REQUEST_AUTHENTICATION_HEADER)
        .withTimeHeaderValue(CLIENT_REQUEST_TIME_HEADER).withMessagePayload(CLIENT_REQUEST_PAYLOAD)
        .withResourcePath(CLIENT_REQUEST_PATH).build();
  }

  @Test
  public void shouldNotAllowToCreateRequestWithoutRequestTimeHeader() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Time header value cannot be null or empty.");

    MAuthRequest.Builder.get().withHttpMethod(CLIENT_REQUEST_METHOD)
        .withAuthenticationHeaderValue(CLIENT_REQUEST_AUTHENTICATION_HEADER)
        .withMessagePayload(CLIENT_REQUEST_PAYLOAD).withResourcePath(CLIENT_REQUEST_PATH).build();
  }

  @Test
  public void shouldNotAllowToCreateRequestWithNegativeRequestTime() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Request time cannot be negative or 0.");

    MAuthRequest.Builder.get().withHttpMethod(CLIENT_REQUEST_METHOD)
        .withAuthenticationHeaderValue(CLIENT_REQUEST_AUTHENTICATION_HEADER)
        .withTimeHeaderValue("-1").withMessagePayload(CLIENT_REQUEST_PAYLOAD)
        .withResourcePath(CLIENT_REQUEST_PATH).build();
  }

  @Test
  public void shouldNotAllowToCreateRequestWithoutRequestPath() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Resource path cannot be null or empty.");

    MAuthRequest.Builder.get().withHttpMethod(CLIENT_REQUEST_METHOD)
        .withAuthenticationHeaderValue(CLIENT_REQUEST_AUTHENTICATION_HEADER)
        .withTimeHeaderValue(CLIENT_REQUEST_TIME_HEADER).withMessagePayload(CLIENT_REQUEST_PAYLOAD)
        .build();
  }

  @Test
  public void shouldCorrectlyCreateMAuthRequest() {
    MAuthRequest request = MAuthRequest.Builder.get()
        .withAuthenticationHeaderValue(CLIENT_REQUEST_AUTHENTICATION_HEADER)
        .withTimeHeaderValue(CLIENT_REQUEST_TIME_HEADER).withHttpMethod(CLIENT_REQUEST_METHOD)
        .withMessagePayload(CLIENT_REQUEST_PAYLOAD).withResourcePath(CLIENT_REQUEST_PATH).build();

    assertThat(request.getAppUUID(), equalTo(UUID.fromString(CLIENT_APP_UUID)));
    assertThat(request.getHttpMethod(), equalTo(CLIENT_REQUEST_METHOD));
    assertThat(request.getResourcePath(), equalTo(CLIENT_REQUEST_PATH));
    assertThat(request.getRequestTime(), equalTo(Long.parseLong(CLIENT_REQUEST_TIME_HEADER)));
    assertThat(request.getMessagePayload(), equalTo(CLIENT_REQUEST_PAYLOAD));
  }

  @Test
  public void shouldCorrectlyCreateMAuthRequestWithoutMessagePayload() {
    MAuthRequest request = MAuthRequest.Builder.get()
        .withAuthenticationHeaderValue(CLIENT_REQUEST_AUTHENTICATION_HEADER)
        .withTimeHeaderValue(CLIENT_REQUEST_TIME_HEADER).withHttpMethod(CLIENT_REQUEST_METHOD)
        .withResourcePath(CLIENT_REQUEST_PATH).build();

    assertThat(request.getAppUUID(), equalTo(UUID.fromString(CLIENT_APP_UUID)));
    assertThat(request.getHttpMethod(), equalTo(CLIENT_REQUEST_METHOD));
    assertThat(request.getResourcePath(), equalTo(CLIENT_REQUEST_PATH));
    assertThat(request.getRequestTime(), equalTo(Long.parseLong(CLIENT_REQUEST_TIME_HEADER)));
    assertThat(request.getMessagePayload(), equalTo(new byte[] {}));
  }

}
