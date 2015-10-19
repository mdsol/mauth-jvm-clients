package com.mdsol.mauth.api;

import com.mdsol.mauth.domain.MAuthRequest;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;

import java.util.Map;

public interface MAuthService {

  /**
   * Performs the validation of the incoming HTTP request by delegating to {@code MAuthValidator}.
   * <p/>
   * The validation process consists of recreating the mAuth hashed signature from the request data
   * and comparing it to the decrypted hash signature from the mAuth header.
   * 
   * @param mAuthRequest Data from the incoming request necessary to perform the validation.
   * @return True or false indicating if the request is valid or not with respect to mAuth.
   */
  boolean validate(MAuthRequest mAuthRequest);

  /**
   * Generates the mAuth headers from the provided request data by delegating to {@code MAuthSigner}
   * .
   * <p/>
   * NOTE: mAuth headers are time sensitive. The headers must be verified by the receiving service
   * within 5 minutes of being generated, or the request will fail.
   *
   * @param httpVerb The HTTP verb of the request, e.g. GET or POST
   * @param requestPath The path of the request, not including protocol, host or query parameters.
   * @param requestBody The body of the request
   * @return MAuth headers which should be appended to the request before sending.
   */
  public Map<String, String> generateRequestHeaders(String httpVerb, String requestPath,
      String requestBody);

  /**
   * Convenience method for clients using Apache {@link HttpClient}. Delegates to
   * {@code MAuthSigner} to generate mAuth headers and append them to the provided
   * {@link HttpUriRequest}.
   * <p/>
   * NOTE: mAuth headers are time sensitive. The headers must be verified by the receiving service
   * within 5 minutes of being generated, or the request will fail.
   *
   * @param request HttpUriRequest, e.g. HttpGet or HttpPost
   */
  public void signRequest(HttpUriRequest request);

}
