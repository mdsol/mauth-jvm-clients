package com.mdsol.mauth.test.utils;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.Charset;

public class FixturesLoader {

  public static String getPublicKey() {
    try {
      return IOUtils.toString(FixturesLoader.class.getResourceAsStream("/keys/fake_publickey.pem"), Charset.defaultCharset());
    } catch (IOException ex) {
      throw new IllegalStateException("Unable to load the public key.", ex);
    }
  }

  public static String getPublicKey2() {
    try {
      return IOUtils.toString(FixturesLoader.class.getResourceAsStream("/keys/fake_publickey2.pem"), Charset.defaultCharset());
    } catch (IOException ex) {
      throw new IllegalStateException("Unable to load the public key2.", ex);
    }
  }

  public static String getPrivateKey() {
    try {
      return IOUtils.toString(FixturesLoader.class.getResourceAsStream("/keys/fake_privatekey.pem"), Charset.defaultCharset());
    } catch (IOException ex) {
      throw new IllegalStateException("Unable to load the private key.", ex);
    }
  }

  public static String getPrivateKey2() {
    try {
      return IOUtils.toString(FixturesLoader.class.getResourceAsStream("/keys/fake_privatekey2.pem"), Charset.defaultCharset());
    } catch (IOException ex) {
      throw new IllegalStateException("Unable to load the private key2.", ex);
    }
  }

  public static byte[] getBinaryFileBody() {
    try {
       return IOUtils.toByteArray(FixturesLoader.class.getResourceAsStream("/blank.jpeg"));
    } catch (IOException ex) {
      throw new IllegalStateException("Unable to load blank.jpeg.", ex);
    }
  }
}