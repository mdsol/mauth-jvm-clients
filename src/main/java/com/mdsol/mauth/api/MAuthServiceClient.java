package com.mdsol.mauth.api;

import com.mdsol.mauth.domain.MAuthConfiguration;
import com.mdsol.mauth.domain.MAuthRequest;
import com.mdsol.mauth.exceptions.MAuthSigningException;
import com.mdsol.mauth.internals.client.MAuthHttpClient;
import com.mdsol.mauth.internals.signer.MAuthSignerImpl;
import com.mdsol.mauth.internals.signer.MAuthSigner;
import com.mdsol.mauth.internals.utils.CurrentEpochTime;
import com.mdsol.mauth.internals.utils.EpochTime;
import com.mdsol.mauth.internals.validator.MAuthValidator;
import com.mdsol.mauth.internals.validator.MAuthValidatorImpl;

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
    this.signer = new MAuthSignerImpl(configuration.getAppUUID(), configuration.getPrivateKey());
    this.validator = new MAuthValidatorImpl(new MAuthHttpClient(configuration, signer), epochTime);
  }

  @Override
  public boolean validate(MAuthRequest mAuthRequest) {
    return validator.validate(mAuthRequest);
  }

  @Override
  public Map<String, String> generateRequestHeaders(String httpVerb, String requestPath,
      String requestBody) throws MAuthSigningException {
    return signer.generateRequestHeaders(httpVerb, requestPath, requestBody);
  }

  @Override
  public void signRequest(HttpUriRequest request) throws MAuthSigningException {
    signer.signRequest(request);
  }

}
