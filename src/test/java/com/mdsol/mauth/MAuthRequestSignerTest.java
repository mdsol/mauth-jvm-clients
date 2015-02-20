package com.mdsol.mauth;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.lang.SecurityException;
import java.util.UUID;

public class MAuthRequestSignerTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public final void constructorWithInvalidKeyStringThrowsException() throws IOException {
    String appUUID = UUID.randomUUID().toString();
    String privateKeyString = "This is not a valid key";
    thrown.expect(SecurityException.class);
    thrown.expectMessage("Unable to process private key string");
    MAuthRequestSigner mAuthRequestSigner = new MAuthRequestSigner(appUUID, privateKeyString);
  }

}
