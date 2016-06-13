package com.mdsol.mauth.utils;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.Charset;

public class FixturesLoader {

  public static String getPublicKey() {
    try {
      return IOUtils.toString(FixturesLoader.class.getResourceAsStream("/keys/publickey.pem"), Charset.defaultCharset());
    } catch (IOException ex) {
      throw new IllegalStateException("Unable to load the public key.", ex);
    }
  }

  public static String getPrivateKey() {
    try {
      return IOUtils.toString(FixturesLoader.class.getResourceAsStream("/keys/privatekey.pem"), Charset.defaultCharset());
    } catch (IOException ex) {
      throw new IllegalStateException("Unable to load the private key.", ex);
    }
  }

}
