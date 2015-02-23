package com.mdsol.mauth;

import static org.junit.Assert.fail;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.lang.SecurityException;
import java.util.UUID;


/**
 * Tests for {@link com.mdsol.mauth.MAuthRequestSigner}
 *
 * @author Jonathan Price <jprice@mdsol.com>
 */
public class MAuthRequestSignerTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public final void constructorWithInvalidKeyStringThrowsException() throws IOException {
    UUID appUUID = UUID.randomUUID();
    String privateKeyString = "This is not a valid key";
    thrown.expect(SecurityException.class);
    thrown.expectMessage("Unable to process private key string");
    new MAuthRequestSigner(appUUID, privateKeyString);
    fail(); // Shouldn't get here - exception will have been thrown
  }

}
