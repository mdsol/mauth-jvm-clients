package com.mdsol.mauth.api;

import com.mdsol.mauth.domain.MAuthConfiguration;
import com.mdsol.mauth.domain.MAuthRequest;
import com.mdsol.mauth.internals.client.MAuthHttpClient;
import com.mdsol.mauth.internals.signer.MAuthSigner;
import com.mdsol.mauth.internals.signer.MAuthSignerImpl;
import com.mdsol.mauth.internals.utils.CurrentEpochTime;
import com.mdsol.mauth.internals.utils.EpochTime;
import com.mdsol.mauth.internals.validator.MAuthValidator;
import com.mdsol.mauth.internals.validator.MAuthValidatorImpl;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.http.client.methods.HttpUriRequest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;
import java.util.Map;

import static com.mdsol.mauth.domain.MAuthConfiguration.SECTION_HEADER;

/**
 * Thread-safe implementation of MAuthService which delegates responsibilities to the
 * {@link MAuthValidator} and {@link MAuthSigner}.
 */
public class MAuthServiceClient implements MAuthService {

  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  private final MAuthSigner signer;
  private final MAuthValidator validator;

  public MAuthServiceClient() {
    this(ConfigFactory.load());
  }

  public MAuthServiceClient(Config config) {
    this(MAuthConfiguration.Builder.parse(config));
    config.checkValid(ConfigFactory.defaultReference(), SECTION_HEADER);
  }

  public MAuthServiceClient(MAuthConfiguration configuration) {
    if (configuration == null) {
      throw new IllegalArgumentException("MAuth configuration cannot be null.");
    }
    EpochTime epochTime = new CurrentEpochTime();
    this.signer = new MAuthSignerImpl(configuration.getAppUUID(), configuration.getPrivateKey(), epochTime);
    this.validator = new MAuthValidatorImpl(new MAuthHttpClient(configuration, signer), epochTime);
  }

  @Override
  public boolean validate(MAuthRequest mAuthRequest) {
    return validator.validate(mAuthRequest);
  }

  @Override
  public Map<String, String> generateRequestHeaders(String httpVerb, String requestPath,
      String requestPayload) {
    return signer.generateRequestHeaders(httpVerb, requestPath, requestPayload);
  }

  @Override
  public void signRequest(HttpUriRequest request) {
    signer.signRequest(request);
  }
}
