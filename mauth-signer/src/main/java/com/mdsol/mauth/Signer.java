package com.mdsol.mauth;

import com.mdsol.mauth.exceptions.MAuthSigningException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;

import java.util.Map;

public interface Signer {

  /**
   * Generates the mAuth headers from the provided HTTP request data.
   * <p/>
   * NOTE: mAuth headers are time sensitive. The headers must be verified by the receiving service
   * within 5 minutes of being generated otherwise the request will fail.
   *
   * @param httpVerb The HTTP verb of the request, e.g. GET, POST, etc.
   * @param requestPath The path of the request, not including protocol, host or query parameters.
   * @param requestPayload The payload of the request
   * @return MAuth headers which should be appended to the request before sending.
   * @throws MAuthSigningException
   */
  Map<String, String> generateRequestHeaders(String httpVerb, String requestPath,
                                             String requestPayload) throws MAuthSigningException;
}
