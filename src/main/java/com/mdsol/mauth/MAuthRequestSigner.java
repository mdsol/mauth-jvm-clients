package com.mdsol.mauth;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;

import java.io.IOException;
import java.io.StringReader;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author jprice
 */
public class MAuthRequestSigner {

  private static EpochTime epochTime = new CurrentEpochTime();

  public static MAuthRequestSigner getDefaultRequestSigner() {
    return defaultRequestSigner;
  }

  public static void setDefaultRequestSigner(MAuthRequestSigner defaultRequestSigner) {
    MAuthRequestSigner.defaultRequestSigner = defaultRequestSigner;
  }

  private static MAuthRequestSigner defaultRequestSigner = null;

  /**
   * Allows replacement of the EpochTime object used for constructing headers, for testing purposes only
   * @param epochTime An object of a class the implements the EpochTime interface
   */
  public static void setEpochTime(EpochTime epochTime) {
    MAuthRequestSigner.epochTime = epochTime;
  }

  private final UUID _appUUID;
  private final PrivateKey _privateKey;

  private final MAuthSignatureHelper _mAuthSignatureHelper;

  public MAuthRequestSigner(UUID appUUID, PrivateKey privateKey) {
    _appUUID = appUUID;
    _privateKey = privateKey;
    _mAuthSignatureHelper = new MAuthSignatureHelper();
  }

  public MAuthRequestSigner(UUID appUUID, String privateKey) throws SecurityException, IOException {
    this(appUUID, getPrivateKeyFromString(privateKey));
  }

  private static PrivateKey getPrivateKeyFromString(String privateKey) throws SecurityException, IOException {
    Security.addProvider(new BouncyCastleProvider());
    PrivateKey pk;
    try (PEMReader reader = new PEMReader(new StringReader(privateKey))) {
      KeyPair kp = (KeyPair) reader.readObject();
      pk = kp.getPrivate();
    } catch (Exception caughtEx) {
      SecurityException ex = new SecurityException("Unable to process private key string");
      ex.initCause(caughtEx);
      throw ex;
    }
    return pk;
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
    String epochTimeString = String.valueOf(epochTime.getSeconds());

    String unencryptedHeaderString =
      _mAuthSignatureHelper.generateUnencryptedHeaderString(_appUUID, httpVerb, requestPath,
        requestBody, epochTimeString);
    String encryptedHeaderString = _mAuthSignatureHelper.encryptHeaderString(_privateKey,
      unencryptedHeaderString);

    HashMap<String, String> headers = new HashMap<>();
    headers.put("x-mws-authentication", "MWS " + _appUUID.toString() + ":" + encryptedHeaderString);
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
  public void signRequest(HttpUriRequest request)
    throws IOException, GeneralSecurityException, CryptoException {
    String httpVerb = request.getMethod();
    String body = "";
    if (request instanceof HttpEntityEnclosingRequest) {
      body = EntityUtils.toString(((HttpEntityEnclosingRequest) request).getEntity());
    }
    Map<String, String> mauthHeaders = generateHeaders(httpVerb, request.getURI().getPath(), body);
    for (String key : mauthHeaders.keySet()) {
      request.addHeader(key, mauthHeaders.get(key));
    }
  }

}
