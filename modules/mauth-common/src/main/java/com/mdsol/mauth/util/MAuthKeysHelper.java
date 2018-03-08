package com.mdsol.mauth.util;

import com.mdsol.mauth.exceptions.MAuthKeyException;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import java.io.IOException;
import java.io.StringReader;
import java.security.PrivateKey;
import java.security.PublicKey;

public class MAuthKeysHelper {

  private static final JcaPEMKeyConverter KEY_CONVERTER =
      new JcaPEMKeyConverter().setProvider("BC");

  public static PublicKey getPublicKeyFromString(final String publicKeyAsString) {
    PublicKey key;
    try (PEMParser parser = new PEMParser(new StringReader(publicKeyAsString))) {
      key = KEY_CONVERTER.getPublicKey((SubjectPublicKeyInfo) parser.readObject());
    } catch (IOException ex) {
      throw new MAuthKeyException("Unable to process public key string", ex);
    }
    return key;
  }

  public static PrivateKey getPrivateKeyFromString(final String privateKeyAsString) {
    PrivateKey pk;
    try (PEMParser parser = new PEMParser(new StringReader(privateKeyAsString))) {
      PEMKeyPair keyPair = (PEMKeyPair) parser.readObject();
      if (keyPair != null) {
        pk = KEY_CONVERTER.getPrivateKey(keyPair.getPrivateKeyInfo());
      } else {
        throw new MAuthKeyException("Unable to process private key string");
      }
    } catch (IOException ex) {
      throw new MAuthKeyException("Unable to process private key string", ex);
    }
    return pk;
  }
}
