package com.mdsol.mauth.internals.signer;

import com.mdsol.mauth.exceptions.MAuthSigningException;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;

import java.util.Map;

public interface MAuthSigner {

  /**
   * Generates the mAuth headers from the provided request data.
   * <p/>
   * NOTE: mAuth headers are time sensitive. The headers must be verified by the receiving service
   * within 5 minutes of being generated, or the request will fail.
   *
   * @param httpVerb The HTTP verb of the request, e.g. GET or POST
   * @param requestPath The path of the request, not including protocol, host or query parameters.
   * @param requestBody The body of the request
   * @return MAuth headers which should be appended to the request before sending.
   * @throws MAuthSigningException
   */
  public Map<String, String> generateHeaders(String httpVerb, String requestPath,
      String requestBody) throws MAuthSigningException;

  /**
   * Convenience method for clients using Apache {@link HttpClient}. Generates mAuth headers and
   * appends them to the provided {@link HttpUriRequest}.
   * <p/>
   * NOTE: mAuth headers are time sensitive. The headers must be verified by the receiving service
   * within 5 minutes of being generated, or the request will fail.
   *
   * @param request HttpUriRequest, e.g. HttpGet or HttpPost
   * @throws MAuthSigningException
   */
  public void signRequest(HttpUriRequest request) throws MAuthSigningException;


}
