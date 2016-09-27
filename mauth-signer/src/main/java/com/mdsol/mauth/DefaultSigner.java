package com.mdsol.mauth;

import com.mdsol.mauth.exceptions.MAuthSigningException;
import com.mdsol.mauth.util.CurrentEpochTimeProvider;
import com.mdsol.mauth.util.EpochTimeProvider;
import com.mdsol.mauth.util.MAuthHeadersHelper;
import com.mdsol.mauth.util.MAuthSignatureHelper;
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
  private final EpochTimeProvider epochTimeProvider;

  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  public DefaultSigner(MAuthConfiguration configuration) {
    this(configuration.getAppUUID(), configuration.getPrivateKey(), new CurrentEpochTimeProvider());
  }

  public DefaultSigner(UUID appUUID, String privateKey, EpochTimeProvider epochTimeProvider) {
    this(appUUID, getPrivateKeyFromString(privateKey), epochTimeProvider);
  }

  public DefaultSigner(UUID appUUID, PrivateKey privateKey, EpochTimeProvider epochTimeProvider) {
    this.appUUID = appUUID;
    this.privateKey = privateKey;
    this.epochTimeProvider = epochTimeProvider;
  }

  @Override
  public Map<String, String> generateRequestHeaders(String httpVerb, String requestPath, String requestPayload) throws MAuthSigningException {
    if (null == requestPayload) {
      requestPayload = "";
    }
    // mAuth uses an epoch time measured in seconds
    long currentTime = epochTimeProvider.inSeconds();

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
