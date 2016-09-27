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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class DefaultSignerTest implements BaseSignerTest {
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
    assertEquals("Time in header does not equal expected test time", String.valueOf(TEST_EPOCH_TIME), headers.get(MAUTH_TIME_HEADER));
  }

  @Test
  public final void generateHeadersIncludesExpectedAuthenticationHeader() throws Exception {
    Map<String, String> headers = mAuthRequestSigner.generateRequestHeaders("GET", "/", null);
    assertEquals("Authentication header does not match expected value", EXPECTED_GET_AUTHENTICATION_HEADER, headers.get(MAUTH_AUTHENTICATION_HEADER));
  }

  @Test
  public final void generateHeadersWithBodyIncludesExpectedAuthenticationHeader() throws Exception {
    Map<String, String> headers = mAuthRequestSigner.generateRequestHeaders("POST", "/", TEST_REQUEST_BODY);
    assertEquals("Authentication header does not match expected value", EXPECTED_POST_AUTHENTICATION_HEADER, headers.get(MAUTH_AUTHENTICATION_HEADER));
  }
}