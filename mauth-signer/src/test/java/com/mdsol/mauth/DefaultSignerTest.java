package com.mdsol.mauth;

import com.mdsol.mauth.exceptions.MAuthKeyException;
import com.mdsol.mauth.test.utils.FixturesLoader;
import com.mdsol.mauth.util.EpochTimeProvider;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.security.Security;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class DefaultSignerTest {
  private long TEST_EPOCH_TIME = 1424700000L;
  private String MAUTH_AUTHENTICATION_HEADER = "x-mws-authentication";
  private UUID testUUID = UUID.fromString("2a6790ab-f6c6-45be-86fc-9e9be76ec12a");

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private static String privateKeyString;
  private Signer mAuthRequestSigner;
  private EpochTimeProvider mockEpochTimeProvider = mock(EpochTimeProvider.class);

  @BeforeClass
  public static void setUpClass() throws Exception {
    privateKeyString = FixturesLoader.getPrivateKey();
    Security.addProvider(new BouncyCastleProvider());
  }

  @Before
  public void setUp() throws Exception {
    when(mockEpochTimeProvider.inSeconds()).thenReturn(TEST_EPOCH_TIME);
    mAuthRequestSigner = new DefaultSigner(testUUID, privateKeyString, mockEpochTimeProvider);
  }

  @Test
  public final void constructorWithInvalidKeyStringThrowsException() throws Exception {
    String privateKeyString = "This is not a valid key";
    thrown.expect(MAuthKeyException.class);
    thrown.expectMessage("Unable to process private key string");
    new DefaultSigner(testUUID, privateKeyString, mockEpochTimeProvider);
    fail("Expected exception not thrown");
  }

  @Test
  public final void generateHeadersIncludesTimeHeaderWithCorrectTime() throws Exception {
    Map<String, String> headers = mAuthRequestSigner.generateRequestHeaders("GET", "/", null);
    String MAUTH_TIME_HEADER = "x-mws-time";
    assertEquals("Time in header does not equal expected test time", String.valueOf(TEST_EPOCH_TIME), headers.get(MAUTH_TIME_HEADER));
  }

  @Test
  public final void generateHeadersIncludesExpectedAuthenticationHeader() throws Exception {
    Map<String, String> headers = mAuthRequestSigner.generateRequestHeaders("GET", "/", null);
    String EXPECTED_GET_AUTHENTICATION_HEADER = "MWS 2a6790ab-f6c6-45be-86fc-9e9be76ec12a:bXkxaWM5Src65bVPdv466zC9JIy79aNfjjTczXoT01Tycxkbv/8U/7utTV+HgdJvvA1Du9wDD+l0dhvRb3lmEI1LIp1A4j2rogHc13n9WdV8Q9x381Te7B9uTSdOz1k/9QRZaDrmFl9GtBq4xe9xQPPF/U0cOFm4R/0OMQCYamf4/mc2PZ6t8ZOCd2gGvR70l1n9PoTTSZaULcul/oR7HFK25FPjsIQ9FkYVjJ+iwKPhrIgcZwUznNL71d+V8bQ2Jr3RK+1c115rlHEy9SgLh1nW8SHP+uzZMApWEFASaLyTePbuvVUDtJbziWYjVvr4m20PM2aLhMmVYcKU5T288w==";
    assertEquals("Authentication header does not match expected value", EXPECTED_GET_AUTHENTICATION_HEADER, headers.get(MAUTH_AUTHENTICATION_HEADER));
  }

  @Test
  public final void generateHeadersWithBodyIncludesExpectedAuthenticationHeader() throws Exception {
    String TEST_REQUEST_BODY = "Request Body";
    Map<String, String> headers = mAuthRequestSigner.generateRequestHeaders("POST", "/", TEST_REQUEST_BODY);
    String EXPECTED_POST_AUTHENTICATION_HEADER = "MWS 2a6790ab-f6c6-45be-86fc-9e9be76ec12a:aDItoM9IOknNhPKH9aqMguASxjBErA2KzCfiZKjCQx0LyMuNZAQ/6tZWfLZ6tI+XMTV51sxc4qiSpuL6UHK9WomqhPtvSDCJ7KU3Xpoi9iJ4J3VtXu8lxKZYkrUBpV0jttbhRn1H3I7VHwXCdV5ptY3WEL9u1iF3whLqUKGyYxf7WFgJmBbX/V7VIRGOW8BJjfWJ9pDVypVBN/VOYWLlKv9o3TTZuuEBtutuBSd6cU4oTMDnQmGkWs9fDAfdkF2l/ZdmD7LFryk9vuyPJ5ur82ksJIZO61fzsEh0Klg/Qcr1E9M0dj0DtxBzw4W0Oc1sXH67xKrKidr3JxuBXjv5gg==";
    assertEquals("Authentication header does not match expected value", EXPECTED_POST_AUTHENTICATION_HEADER, headers.get(MAUTH_AUTHENTICATION_HEADER));
  }
}