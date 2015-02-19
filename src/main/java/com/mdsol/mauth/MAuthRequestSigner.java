package com.mdsol.mauth;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.encodings.PKCS1Encoding;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;

import java.io.IOException;
import java.io.StringReader;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jprice
 *
 */
public class MAuthRequestSigner {

  private final String _appUUID;
  private final String _privateKeyString;
  private final PrivateKey _privateKey;

  public MAuthRequestSigner(String appUUID, String privateKey) throws IOException {
    _appUUID = appUUID;
    _privateKeyString = privateKey;

    // Generate the private key from the string
    PEMReader reader = null;
    try {
      reader = new PEMReader(new StringReader(_privateKeyString));
      KeyPair kp = (KeyPair) reader.readObject();
      _privateKey = kp.getPrivate();
    } finally {
      if (reader != null) {
        reader.close();
      }
    }
  }

  public Map<String, String> generateHeaders(String httpVerb, String requestURL, String requestBody) throws GeneralSecurityException, IOException, CryptoException {
    // mAuth uses an epoch time measured in seconds
    String epochTimeString = String.valueOf(System.currentTimeMillis() / 1000);

    String unencryptedHeaderString =
        generatedUnencryptedHeaderString(httpVerb, requestURL, requestBody, epochTimeString);
    String encryptedHeaderString = encryptHeaderString(unencryptedHeaderString);

    HashMap<String, String> headers = new HashMap<String, String>();
    headers.put("x-mws-authentication", "MWS " + _appUUID + ":" + encryptedHeaderString);
    headers.put("x-mws-time", epochTimeString);
    return headers;
  }

  public void signRequest(HttpMethod request) throws IOException, GeneralSecurityException, CryptoException {
    String httpVerb = request.getName();
    String body = "";
    if (httpVerb.equals("POST")) {
      ByteArrayOutputStream oStream = new ByteArrayOutputStream();
      ((PostMethod) request).getRequestEntity().writeRequest(oStream);
    }
    Map<String, String> mauthHeaders = generateHeaders(httpVerb, request.getURI().toString(), body);
    for (String key : mauthHeaders.keySet()) {
      request.addRequestHeader(key, mauthHeaders.get(key));
    }
  }

  private String generatedUnencryptedHeaderString(String httpVerb, String resourceUrl, String body,
      String epochTime) {
    String unencryptedHeaderString =
        httpVerb + "\n" + resourceUrl + "\n" + body + "\n" + _appUUID + "\n" + epochTime;
    return unencryptedHeaderString;
  }

  private String encryptHeaderString(String unencryptedString) throws GeneralSecurityException, IOException, CryptoException {
    //Get digest
    Security.addProvider(new BouncyCastleProvider());
    
    MessageDigest md = MessageDigest.getInstance("SHA-512", "BC");
    byte[] digestedString = md.digest(unencryptedString.getBytes());
    
    //Convert to hex
    String hexEncodedString = Hex.encodeHexString(digestedString);
    
    //encrypt
    PKCS1Encoding encryptEngine = new PKCS1Encoding(new RSAEngine());
    encryptEngine.init(true, PrivateKeyFactory.createKey(_privateKey.getEncoded()));
    byte[] encryptedStringBytes = encryptEngine.processBlock(hexEncodedString.getBytes(), 0, hexEncodedString.getBytes().length);
    
    //Base64 encode
    String encryptedHeaderString = Base64.encodeBase64String(encryptedStringBytes);
    
    return encryptedHeaderString;
  }

}
