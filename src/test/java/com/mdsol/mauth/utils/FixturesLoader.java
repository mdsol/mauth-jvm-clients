package com.mdsol.mauth.utils;

import org.apache.commons.io.IOUtils;

import java.io.IOException;

public class FixturesLoader {

  public static String getPublicKey() {
    try {
      return IOUtils.toString(FixturesLoader.class.getResourceAsStream("/keys/publickey.pem"));
    } catch (IOException ex) {
      throw new IllegalStateException("Unable to load the public key.", ex);
    }
  }

  public static String getPrivateKey() {
    try {
      return IOUtils.toString(FixturesLoader.class.getResourceAsStream("/keys/privatekey.pem"));
    } catch (IOException ex) {
      throw new IllegalStateException("Unable to load the private key.", ex);
    }
  }

}
