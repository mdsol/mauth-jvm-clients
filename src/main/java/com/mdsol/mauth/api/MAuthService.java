package com.mdsol.mauth.api;

import com.mdsol.mauth.domain.MAuthRequest;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;

import java.util.Map;

/**
 * Entry point to the MAuth Java client service.
 */
public interface MAuthService {

  /**
   * Performs the validation of an incoming HTTP request.
   * <p/>
   * The validation process consists of recreating the mAuth hashed signature from the request data
   * and comparing it to the decrypted hash signature from the mAuth header.
   * 
   * @param mAuthRequest Data from the incoming HTTP request necessary to perform the validation.
   * @return True or false indicating if the request is valid or not with respect to mAuth.
   */
  boolean validate(MAuthRequest mAuthRequest);

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
   */
  public Map<String, String> generateRequestHeaders(String httpVerb, String requestPath,
      String requestPayload);

  /**
   * Convenience method for clients using Apache {@link HttpClient}. Generates mAuth headers and
   * includes them into the provided {@link HttpUriRequest}.
   * <p/>
   * NOTE: mAuth headers are time sensitive. The headers must be verified by the receiving service
   * within 5 minutes of being generated otherwise the request will fail.
   *
   * @param request {@link HttpUriRequest}, e.g. {@link HttpGet} or {@link HttpPost}
   */
  public void signRequest(HttpUriRequest request);
}
