package com.mdsol.mauth.api.impl;

import com.mdsol.mauth.api.MAuthService;
import com.mdsol.mauth.domain.MAuthConfiguration;
import com.mdsol.mauth.domain.MAuthRequest;
import com.mdsol.mauth.exceptions.MAuthSigningException;
import com.mdsol.mauth.services.MAuthHttpClient;
import com.mdsol.mauth.services.MAuthRequestSigner;
import com.mdsol.mauth.services.MAuthSigner;
import com.mdsol.mauth.services.MAuthValidator;
import com.mdsol.mauth.services.MAuthValidatorImpl;
import com.mdsol.mauth.utils.CurrentEpochTime;
import com.mdsol.mauth.utils.EpochTime;

import org.apache.http.client.methods.HttpUriRequest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;
import java.util.Map;

public class MAuthServiceClient implements MAuthService {

  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  private final MAuthSigner signer;
  private final MAuthValidator validator;

  public MAuthServiceClient(MAuthConfiguration configuration) {
    if (configuration == null) {
      throw new IllegalArgumentException("MAuth configuration cannot be null.");
    }
    EpochTime epochTime = new CurrentEpochTime();
    this.signer = new MAuthRequestSigner(configuration.getAppUUID(), configuration.getPrivateKey());
    this.validator = new MAuthValidatorImpl(new MAuthHttpClient(configuration, signer), epochTime);
  }

  @Override
  public boolean validate(MAuthRequest mAuthRequest) {
    return validator.validate(mAuthRequest);
  }

  @Override
  public Map<String, String> generateHeaders(String httpVerb, String requestPath,
      String requestBody) throws MAuthSigningException {
    return signer.generateHeaders(httpVerb, requestPath, requestBody);
  }

  @Override
  public void signRequest(HttpUriRequest request) throws MAuthSigningException {
    signer.signRequest(request);
  }

}
