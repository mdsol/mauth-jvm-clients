package com.mdsol.mauth.fake.services;

import com.mdsol.mauth.MAuthRequest;
import com.mdsol.mauth.Signer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Map;

@Service
public class AuthenticatorService {

  private String defaultHostToForward;
  private Signer signer;

  @Autowired
  public AuthenticatorService(Signer signer, @Value("${defaultHostToForward}") String defaultHostToForward) {
    this.signer = signer;
    this.defaultHostToForward = defaultHostToForward;
  }

  public RequestEntity<String> createModifiedRequest(RequestEntity<String> entity,
                                                     String overridenUrlToForward, String contentType) {
    URI urlToForward = createUrlToForward(entity, overridenUrlToForward);
    Map<String, String> mAuthHeaders = signer.generateRequestHeaders(entity.getMethod().name(), urlToForward.getPath(), entity.getBody());
    return modifyRequest(entity, mAuthHeaders, urlToForward, contentType);
  }

  private URI createUrlToForward(RequestEntity<String> entity, String overridenUrlToForward) {
    String urlToForward;
    if (overridenUrlToForward == null || overridenUrlToForward.isEmpty()) {
      String pathPart = entity.getUrl().getPath() != null ? entity.getUrl().getPath() : "";
      String queryPart = entity.getUrl().getQuery() != null ? "?" + entity.getUrl().getQuery() : "";
      urlToForward = defaultHostToForward + pathPart + queryPart;
    } else {
      urlToForward = overridenUrlToForward;
    }
    return URI.create(urlToForward);
  }

  private RequestEntity<String> modifyRequest(RequestEntity<String> entity,
                                              Map<String, String> mAuthHeaders, URI urlToForward, String contentType) {
    RequestEntity.BodyBuilder builder = RequestEntity.method(entity.getMethod(), urlToForward);

    entity.getHeaders().forEach((key, value) -> {
      // don't include existing mAuthHeaders (ignore them, since we are generating new ones)
      // don't include content-type (see below)
      if (isNotMAuthHeader(key) && isNotContentTypeHeader(key)) {
        builder.header(key, value.toArray(new String[value.size()]));
      }
    });
    mAuthHeaders.entrySet().forEach((header) -> builder.header(header.getKey(), header.getValue()));

    // content-type in entity gets modified by Spring to append charset at the end, but we want to
    // forward exactly the same as incoming.
    if (contentType != null && !contentType.isEmpty()) {
      builder.header("content-type", contentType);
    }

    return builder.body(entity.getBody());
  }

  private boolean isNotMAuthHeader(String key) {
    return !(key.equalsIgnoreCase(MAuthRequest.MAUTH_AUTHENTICATION_HEADER_NAME)
        || key.equalsIgnoreCase(MAuthRequest.MAUTH_TIME_HEADER_NAME));
  }

  private boolean isNotContentTypeHeader(String key) {
    return !(key.equalsIgnoreCase("content-type"));
  }

}
