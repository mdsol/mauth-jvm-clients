package com.mdsol.mauth.apache;

import com.mdsol.mauth.DefaultSigner;
import com.mdsol.mauth.SignerConfiguration;
import com.mdsol.mauth.exceptions.MAuthSigningException;
import com.mdsol.mauth.util.CurrentEpochTimeProvider;
import com.mdsol.mauth.util.EpochTimeProvider;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.Map;
import java.util.UUID;

public class HttpClientRequestSigner extends DefaultSigner {

  public HttpClientRequestSigner(SignerConfiguration configuration) {
    super(configuration);
  }

  public HttpClientRequestSigner(UUID appUUID, String privateKey) {
    super(appUUID, privateKey);
  }

  public HttpClientRequestSigner(UUID appUUID, String privateKey, EpochTimeProvider epochTimeProvider) {
    super(appUUID, privateKey, epochTimeProvider);
  }

  public HttpClientRequestSigner(UUID appUUID, PrivateKey privateKey) {
    super(appUUID, privateKey, new CurrentEpochTimeProvider());
  }

  public HttpClientRequestSigner(UUID appUUID, PrivateKey privateKey, EpochTimeProvider epochTimeProvider) {
    super(appUUID, privateKey, epochTimeProvider);
  }

  public HttpClientRequestSigner(UUID appUUID, String privateKey, EpochTimeProvider epochTimeProvider, boolean v2OnlySignRequests) {
    super(appUUID, privateKey, epochTimeProvider, v2OnlySignRequests);
  }

  /**
   * Convenience method for clients using Apache {@link HttpClient}. Generates mAuth headers and
   * includes them into the provided {@link HttpUriRequest}.
   *
   * NOTE: mAuth headers are time sensitive. The headers must be verified by the receiving service
   * within 5 minutes of being generated otherwise the request will fail.
   *
   * @param request {@link HttpUriRequest}, e.g. {@link HttpGet} or {@link HttpPost}
   * @throws MAuthSigningException wraps {@link ParseException} and {@link IOException}
   */
  public void signRequest(HttpUriRequest request) throws MAuthSigningException {
    String httpVerb = request.getMethod();
    byte[] body = "".getBytes(StandardCharsets.UTF_8);

    if (request instanceof HttpEntityEnclosingRequest) {
      try {
        body = EntityUtils.toByteArray(((HttpEntityEnclosingRequest) request).getEntity());
      } catch (ParseException | IOException e) {
        throw new MAuthSigningException(e);
      }
    }

    Map<String, String> mauthHeaders = generateRequestHeaders(httpVerb, request.getURI().getRawPath(), body, request.getURI().getRawQuery());
    for (String key : mauthHeaders.keySet()) {
      request.addHeader(key, mauthHeaders.get(key));
    }
  }
}
