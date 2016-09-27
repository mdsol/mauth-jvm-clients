package com.mdsol.mauth;

import com.mdsol.mauth.exceptions.MAuthSigningException;
import com.mdsol.mauth.util.MAuthHeadersHelper;
import com.mdsol.mauth.util.MAuthSignatureHelper;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.mdsol.mauth.util.MAuthKeysHelper.getPrivateKeyFromString;

public class DefaultSigner implements Signer {

  private final UUID appUUID;
  private final PrivateKey privateKey;

  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  public DefaultSigner(UUID appUUID, String privateKey) {
    this(appUUID, getPrivateKeyFromString(privateKey));
  }

  public DefaultSigner(UUID appUUID, PrivateKey privateKey) {
    this.appUUID = appUUID;
    this.privateKey = privateKey;
  }

  @Override
  public Map<String, String> generateRequestHeaders(String httpVerb, String requestPath, String requestPayload) throws MAuthSigningException {
    if (null == requestPayload) {
      requestPayload = "";
    }
    // mAuth uses an epoch time measured in seconds
    long currentTime = System.currentTimeMillis() / 1000;

    String unencryptedSignature = MAuthSignatureHelper.generateUnencryptedSignature(appUUID,
        httpVerb, requestPath, requestPayload, String.valueOf(currentTime));

    String encryptedSignature;
    try {
      encryptedSignature =
          MAuthSignatureHelper.encryptSignature(privateKey, unencryptedSignature);
    } catch (IOException | CryptoException e) {
      throw new MAuthSigningException(e);
    }

    HashMap<String, String> headers = new HashMap<>();
    headers.put(
        MAuthRequest.MAUTH_AUTHENTICATION_HEADER_NAME,
        MAuthHeadersHelper.createAuthenticationHeaderValue(appUUID, encryptedSignature)
    );
    headers.put(MAuthRequest.MAUTH_TIME_HEADER_NAME, MAuthHeadersHelper.createTimeHeaderValue(currentTime));

    return headers;
  }
}
