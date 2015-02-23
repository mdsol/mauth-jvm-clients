package com.mdsol.mauth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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
    Map<String, String> headers = mAuthRequestSigner.generateHeaders("GET", "/", "");
    assertEquals(String.valueOf(TEST_EPOCH_TIME), headers.get(MAUTH_TIME_HEADER));
  }

}
