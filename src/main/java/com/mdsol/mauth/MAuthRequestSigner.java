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
import java.security.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jprice
 */
public class MAuthRequestSigner {

  private final String _appUUID;
  private final PrivateKey _privateKey;

  public MAuthRequestSigner(String appUUID, String privateKey) throws SecurityException, IOException {
    _appUUID = appUUID;

    // Generate the private key from the string
    Security.addProvider(new BouncyCastleProvider());
    PEMReader reader = null;
    try {
      reader = new PEMReader(new StringReader(privateKey));
      KeyPair kp = (KeyPair) reader.readObject();
      _privateKey = kp.getPrivate();
    } catch (Exception caughtEx) {
      SecurityException ex = new SecurityException("Unable to process private key string");
      ex.initCause(caughtEx);
      throw ex;
    } finally {
      if (reader != null) {
        reader.close();
      }
    }
  }

  /**
   * Generates the mAuth headers with the provided request data.
   * <p/>
   * NOTE: mAuth headers are time sensitive. The headers must be verified by the receiving service
   * within 5 minutes of being generated, or the request will fail.
   *
   * @param httpVerb    The HTTP verb of the request, e.g. GET or POST
   * @param requestPath The path of the request, not including protocol, host or query parameters.
   * @param requestBody The body of the request
   * @return
   * @throws GeneralSecurityException
   * @throws IOException
   * @throws CryptoException
   */
  public Map<String, String> generateHeaders(String httpVerb, String requestPath,
    String requestBody) throws GeneralSecurityException, IOException, CryptoException {
    if (null == requestBody) {
      requestBody = "";
    }
    // mAuth uses an epoch time measured in seconds
    String epochTimeString = String.valueOf(System.currentTimeMillis() / 1000);

    String unencryptedHeaderString =
      generateUnencryptedHeaderString(httpVerb, requestPath, requestBody, epochTimeString);
    String encryptedHeaderString = encryptHeaderString(unencryptedHeaderString);

    HashMap<String, String> headers = new HashMap<>();
    headers.put("x-mws-authentication", "MWS " + _appUUID + ":" + encryptedHeaderString);
    headers.put("x-mws-time", epochTimeString);
    return headers;
  }

  /**
   * Adds the mAuth headers to the provided HttpMethod object.
   * <p/>
   * NOTE: mAuth headers are time sensitive. The headers must be verified by the receiving service
   * within 5 minutes of being generated, or the request will fail.
   *
   * @param request
   * @throws IOException
   * @throws GeneralSecurityException
   * @throws CryptoException
   */
  public void signRequest(HttpMethod request)
    throws IOException, GeneralSecurityException, CryptoException {
    String httpVerb = request.getName();
    String body = "";
    if (httpVerb.equals("POST")) {
      ByteArrayOutputStream oStream = new ByteArrayOutputStream();
      ((PostMethod) request).getRequestEntity().writeRequest(oStream);
    }
    Map<String, String> mauthHeaders = generateHeaders(httpVerb, request.getURI().getPath(), body);
    for (String key : mauthHeaders.keySet()) {
      request.addRequestHeader(key, mauthHeaders.get(key));
    }
  }

  private String generateUnencryptedHeaderString(String httpVerb, String resourceUrl, String body,
    String epochTime) {
    return httpVerb + "\n" + resourceUrl + "\n" + body + "\n" + _appUUID + "\n" + epochTime;
  }

  private String encryptHeaderString(String unencryptedString)
    throws GeneralSecurityException, IOException, CryptoException {
    // Get digest
    MessageDigest md = MessageDigest.getInstance("SHA-512", "BC");
    byte[] digestedString = md.digest(unencryptedString.getBytes());

    // Convert to hex
    String hexEncodedString = Hex.encodeHexString(digestedString);

    // encrypt
    PKCS1Encoding encryptEngine = new PKCS1Encoding(new RSAEngine());
    encryptEngine.init(true, PrivateKeyFactory.createKey(_privateKey.getEncoded()));
    byte[] encryptedStringBytes = encryptEngine
      .processBlock(hexEncodedString.getBytes(), 0, hexEncodedString.getBytes().length);

    // Base64 encode
    String encryptedHeaderString = new String(Base64.encodeBase64(encryptedStringBytes), "UTF-8");

    return encryptedHeaderString;
  }

}
