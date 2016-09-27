package com.mdsol.mauth;

import com.mdsol.mauth.test.utils.FixturesLoader;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.security.Security;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class HttpClientRequestSignerTest extends DefaultSignerTest{

  private final UUID testUUID = UUID.fromString("2a6790ab-f6c6-45be-86fc-9e9be76ec12a");
  private HttpClientRequestSigner mAuthRequestSigner;
  private static String privateKeyString;

  @BeforeClass
  public static void setUpClass() throws Exception {
    privateKeyString = FixturesLoader.getPrivateKey();
    Security.addProvider(new BouncyCastleProvider());
  }

  @Before
  public void setUp() throws Exception {
    mAuthRequestSigner = new HttpClientRequestSigner(testUUID, privateKeyString);
  }

  @Test
  public final void signRequestAddsExpectedTimeHeader() throws Exception {
    HttpGet get = new HttpGet("http://mauth.imedidata.com/");
    mAuthRequestSigner.signRequest(get);
    assertEquals("Time in header does not equal expected test time",
        String.valueOf(TEST_EPOCH_TIME), get.getFirstHeader(MAUTH_TIME_HEADER).getValue());
  }

  @Test
  public final void signRequestAddsExpectedAuthenticationHeader() throws Exception {
    HttpGet get = new HttpGet("http://mauth.imedidata.com/");
    mAuthRequestSigner.signRequest(get);
    assertEquals("Authentication header does not match expected value",
        EXPECTED_GET_AUTHENTICATION_HEADER,
        get.getFirstHeader(MAUTH_AUTHENTICATION_HEADER).getValue());
  }

  @Test
  public final void signRequestWithBodyAddsExpectedAuthenticationHeader() throws Exception {
    HttpPost post = new HttpPost("http://mauth.imedidata.com/");
    post.setEntity(new StringEntity(TEST_REQUEST_BODY));
    mAuthRequestSigner.signRequest(post);
    assertEquals("Authentication header does not match expected value",
        EXPECTED_POST_AUTHENTICATION_HEADER,
        post.getFirstHeader(MAUTH_AUTHENTICATION_HEADER).getValue());
  }
}