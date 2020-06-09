package com.mdsol.mauth;

import com.mdsol.mauth.exceptions.MAuthSigningException;

import java.util.Map;

public interface Signer {

  /**
   * Generates the mAuth headers from the provided HTTP request data for Mauth V1.
   *
   * NOTE: mAuth headers are time sensitive. The headers must be verified by the receiving service
   * within 5 minutes of being generated otherwise the request will fail.
   *
   * @deprecated
   * This is used for Mauth V1 protocol only, replaced by {@link #generateRequestHeaders(String, String, byte[], String)} for Mauth V2 protocol
   *
   * @param httpVerb The HTTP verb of the request, e.g. GET, POST, etc.
   * @param requestPath The path of the request, not including protocol, host or query parameters.
   * @param requestPayload The payload of the request
   * @return MAuth headers which should be appended to the request before sending.
   * @throws MAuthSigningException when request cannot be signed
   */
  @Deprecated
  Map<String, String> generateRequestHeaders(String httpVerb, String requestPath,
                                             String requestPayload) throws MAuthSigningException;

  /**
   * Generates the mAuth headers from the provided HTTP request data for Mauth V2(and V1) protocol
   *
   * NOTE: mAuth headers are time sensitive. The headers must be verified by the receiving service
   * within 5 minutes of being generated otherwise the request will fail.
   *
   * @param httpVerb The HTTP verb of the request, e.g. GET, POST, etc.
   * @param requestPath The path of the request, not including protocol, host or query parameters.
   * @param requestPayload The payload of the request
   * @param queryParameters The query parameters (URL-encoded)
   * @return MAuth headers which should be appended to the request before sending.
   * @throws MAuthSigningException when request cannot be signed
   */
  Map<String, String> generateRequestHeaders(String httpVerb,
      String requestPath, byte[] requestPayload, String queryParameters) throws MAuthSigningException;

}
