package com.mdsol.mauth.internals.signer;

import static com.mdsol.mauth.internals.utils.MAuthKeysHelper.getPrivateKeyFromString;

import com.mdsol.mauth.exceptions.MAuthSigningException;
import com.mdsol.mauth.internals.utils.CurrentEpochTime;
import com.mdsol.mauth.internals.utils.EpochTime;
import com.mdsol.mauth.internals.utils.MAuthSignatureHelper;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.crypto.CryptoException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MAuthRequestSigner implements MAuthSigner {

  private static EpochTime epochTime = new CurrentEpochTime();

  /**
   * Allows replacement of the EpochTime object used for constructing headers, for testing purposes
   * only
   * 
   * @param epochTime An object of a class the implements the EpochTime interface
   */
  public static void setEpochTime(EpochTime epochTime) {
    MAuthRequestSigner.epochTime = epochTime;
  }

  private final UUID appUUID;
  private final PrivateKey privateKey;

  public MAuthRequestSigner(UUID appUUID, String privateKey) {
    this(appUUID, getPrivateKeyFromString(privateKey));
  }

  public MAuthRequestSigner(UUID appUUID, PrivateKey privateKey) {
    this.appUUID = appUUID;
    this.privateKey = privateKey;
  }

  public Map<String, String> generateHeaders(String httpVerb, String requestPath,
      String requestBody) throws MAuthSigningException {
    if (null == requestBody) {
      requestBody = "";
    }
    // mAuth uses an epoch time measured in seconds
    String epochTimeString = String.valueOf(epochTime.getSeconds());

    String unencryptedHeaderString = MAuthSignatureHelper.generateUnencryptedHeaderString(appUUID,
        httpVerb, requestPath, requestBody, epochTimeString);

    String encryptedHeaderString;
    try {
      encryptedHeaderString =
          MAuthSignatureHelper.encryptHeaderString(privateKey, unencryptedHeaderString);
    } catch (GeneralSecurityException | IOException | CryptoException e) {
      throw new MAuthSigningException(e);
    }

    HashMap<String, String> headers = new HashMap<>();
    headers.put("x-mws-authentication", "MWS " + appUUID.toString() + ":" + encryptedHeaderString);
    headers.put("x-mws-time", epochTimeString);

    return headers;
  }

  public void signRequest(HttpUriRequest request) throws MAuthSigningException {
    String httpVerb = request.getMethod();
    String body = "";

    if (request instanceof HttpEntityEnclosingRequest) {
      try {
        body = EntityUtils.toString(((HttpEntityEnclosingRequest) request).getEntity());
      } catch (ParseException | IOException e) {
        throw new MAuthSigningException(e);
      }
    }

    Map<String, String> mauthHeaders = generateHeaders(httpVerb, request.getURI().getPath(), body);
    for (String key : mauthHeaders.keySet()) {
      request.addHeader(key, mauthHeaders.get(key));
    }
  }
}
