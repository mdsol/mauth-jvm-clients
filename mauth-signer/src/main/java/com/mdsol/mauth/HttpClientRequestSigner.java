package com.mdsol.mauth;

import com.mdsol.mauth.exceptions.MAuthSigningException;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.security.PrivateKey;
import java.util.Map;
import java.util.UUID;

public class HttpClientRequestSigner extends DefaultSigner {

  public HttpClientRequestSigner(MAuthConfiguration configuration) {
    super(configuration);
  }

  public HttpClientRequestSigner(UUID appUUID, String privateKey) {
    super(appUUID, privateKey);
  }

  public HttpClientRequestSigner(UUID appUUID, PrivateKey privateKey) {
    super(appUUID, privateKey);
  }

  /**
   * Convenience method for clients using Apache {@link HttpClient}. Generates mAuth headers and
   * includes them into the provided {@link HttpUriRequest}.
   * <p/>
   * NOTE: mAuth headers are time sensitive. The headers must be verified by the receiving service
   * within 5 minutes of being generated otherwise the request will fail.
   *
   * @param request {@link HttpUriRequest}, e.g. {@link HttpGet} or {@link HttpPost}
   * @throws MAuthSigningException
   */
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

    Map<String, String> mauthHeaders = generateRequestHeaders(httpVerb, request.getURI().getPath(), body);
    for (String key : mauthHeaders.keySet()) {
      request.addHeader(key, mauthHeaders.get(key));
    }
  }
}
