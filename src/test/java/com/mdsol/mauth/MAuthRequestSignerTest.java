package com.mdsol.mauth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Map;
import java.util.UUID;


/**
 * Tests for {@link com.mdsol.mauth.MAuthRequestSigner}
 *
 * @author Jonathan Price <jprice@mdsol.com>
 */
public class MAuthRequestSignerTest {

  private static String MAUTH_TIME_HEADER = "x-mws-time";
  private static String MAUTH_AUTHENTICATION_HEADER = "x-mws-authentication";

  private static final long TEST_EPOCH_TIME = 1424700000L;
  private static final String TEST_REQUEST_BODY = "Request Body";
  private static final String EXPECTED_GET_AUTHENTICATION_HEADER = "MWS 2a6790ab-f6c6-45be-86fc-9e9be76ec12a:bXkxaWM5Src65bVPdv466zC9JIy79aNfjjTczXoT01Tycxkbv/8U/7utTV+HgdJvvA1Du9wDD+l0dhvRb3lmEI1LIp1A4j2rogHc13n9WdV8Q9x381Te7B9uTSdOz1k/9QRZaDrmFl9GtBq4xe9xQPPF/U0cOFm4R/0OMQCYamf4/mc2PZ6t8ZOCd2gGvR70l1n9PoTTSZaULcul/oR7HFK25FPjsIQ9FkYVjJ+iwKPhrIgcZwUznNL71d+V8bQ2Jr3RK+1c115rlHEy9SgLh1nW8SHP+uzZMApWEFASaLyTePbuvVUDtJbziWYjVvr4m20PM2aLhMmVYcKU5T288w==";
  private static final String EXPECTED_POST_AUTHENTICATION_HEADER = "MWS 2a6790ab-f6c6-45be-86fc-9e9be76ec12a:aDItoM9IOknNhPKH9aqMguASxjBErA2KzCfiZKjCQx0LyMuNZAQ/6tZWfLZ6tI+XMTV51sxc4qiSpuL6UHK9WomqhPtvSDCJ7KU3Xpoi9iJ4J3VtXu8lxKZYkrUBpV0jttbhRn1H3I7VHwXCdV5ptY3WEL9u1iF3whLqUKGyYxf7WFgJmBbX/V7VIRGOW8BJjfWJ9pDVypVBN/VOYWLlKv9o3TTZuuEBtutuBSd6cU4oTMDnQmGkWs9fDAfdkF2l/ZdmD7LFryk9vuyPJ5ur82ksJIZO61fzsEh0Klg/Qcr1E9M0dj0DtxBzw4W0Oc1sXH67xKrKidr3JxuBXjv5gg==";
  private static String privateKeyString;
  private final UUID testUUID = UUID.fromString("2a6790ab-f6c6-45be-86fc-9e9be76ec12a");

  private MAuthRequestSigner mAuthRequestSigner;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @BeforeClass
  public static void setUpClass() throws Exception {
    privateKeyString = IOUtils.toString(MAuthRequestSigner.class.getResourceAsStream("privatekey.pem"), "UTF-8");
  }

  @Before
  public void setUp() throws Exception {
    mAuthRequestSigner = new MAuthRequestSigner(testUUID, privateKeyString);
    EpochTime testEpochTime = new TestEpochTime(TEST_EPOCH_TIME);
    MAuthRequestSigner.setEpochTime(testEpochTime);
  }

  @Test
  public final void constructorWithInvalidKeyStringThrowsException() throws Exception {
    String privateKeyString = "This is not a valid key";
    thrown.expect(SecurityException.class);
    thrown.expectMessage("Unable to process private key string");
    new MAuthRequestSigner(testUUID, privateKeyString);
    fail(); // Shouldn't get here - exception will have been thrown
  }

  @Test
  public final void generateHeadersIncludesTimeHeaderWithCorrectTime() throws Exception {
    Map<String, String> headers = mAuthRequestSigner.generateHeaders("GET", "/", null);
    assertEquals(String.valueOf(TEST_EPOCH_TIME), headers.get(MAUTH_TIME_HEADER));
  }

  @Test
  public final void generateHeadersIncludesExpectedAuthenticationHeader() throws Exception {
    Map<String, String> headers = mAuthRequestSigner.generateHeaders("GET", "/", null);
    assertEquals(EXPECTED_GET_AUTHENTICATION_HEADER, headers.get(MAUTH_AUTHENTICATION_HEADER));
  }

  @Test
  public final void generateHeadersWithBodyIncludesExpectedAuthenticationHeader() throws Exception {
    Map<String, String> headers = mAuthRequestSigner.generateHeaders("POST", "/", TEST_REQUEST_BODY);
    assertEquals(EXPECTED_POST_AUTHENTICATION_HEADER, headers.get(MAUTH_AUTHENTICATION_HEADER));
  }

  @Test
  public final void signRequestAddsExpectedTimeHeader() throws Exception {
    GetMethod getMethod = new GetMethod("http://mauth.imedidata.com/");
    mAuthRequestSigner.signRequest(getMethod);
    assertEquals(String.valueOf(TEST_EPOCH_TIME), getMethod.getRequestHeader(MAUTH_TIME_HEADER).getValue());
  }

  @Test
  public final void signRequestAddsExpectedAuthenticationHeader() throws Exception {
    GetMethod gm = new GetMethod("http://mauth.imedidata.com/");
    mAuthRequestSigner.signRequest(gm);
    assertEquals(EXPECTED_GET_AUTHENTICATION_HEADER,
      gm.getRequestHeader(MAUTH_AUTHENTICATION_HEADER).getValue());
  }

  @Test
  public final void signRequestWithBodyAddsExpectedAuthenticationHeader() throws Exception {
    PostMethod postMethod = new PostMethod("http://mauth.imedidata.com/");
    postMethod.setRequestEntity(new StringRequestEntity(TEST_REQUEST_BODY, null, null));
    mAuthRequestSigner.signRequest(postMethod);
    assertEquals(EXPECTED_POST_AUTHENTICATION_HEADER,
      postMethod.getRequestHeader(MAUTH_AUTHENTICATION_HEADER).getValue());
  }
}
