package com.mdsol.mauth.apache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.mdsol.mauth.AuthenticatorConfiguration;
import com.mdsol.mauth.Signer;
import com.mdsol.mauth.exception.HttpClientPublicKeyProviderException;
import com.mdsol.mauth.util.MAuthKeysHelper;
import com.mdsol.mauth.utils.ClientPublicKeyProvider;

import org.apache.http.*;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class HttpClientPublicKeyProvider implements ClientPublicKeyProvider {

  private static final Logger logger = LoggerFactory.getLogger(HttpClientPublicKeyProvider.class);

  private final AuthenticatorConfiguration configuration;
  private final Signer signer;
  private final CloseableHttpClient httpclient;
  private final PublicKeyResponseHandler publicKeyResponseHandler;
  private LoadingCache<UUID, PublicKeyData> publicKeyCache;

  public HttpClientPublicKeyProvider(AuthenticatorConfiguration configuration, Signer signer) {
    this.configuration = configuration;
    this.signer = signer;
    this.httpclient = HttpClients.createDefault();
    this.publicKeyResponseHandler = new PublicKeyResponseHandler();
    setupCache();
  }

  private void setupCache() {
    publicKeyCache = Caffeine.newBuilder()
        .expireAfter(new Expiry<UUID, PublicKeyData>() {
          public long expireAfterCreate(UUID key, PublicKeyData data, long currentTime) {
            return TimeUnit.SECONDS.toNanos(data.getMaxAgeSeconds());
          }
          public long expireAfterUpdate(UUID key, PublicKeyData data, long currentTime, long currentDuration) {
            return currentDuration;
          }
          public long expireAfterRead(UUID key, PublicKeyData data, long currentTime, long currentDuration) {
            return currentDuration;
          }
        })
        .build(this::getPublicKeyFromMauth);
  }

  private PublicKeyData getPublicKeyFromMauth(UUID appUUID) {
    byte[] payload = new byte[0];
    String requestUrlPath = getRequestUrlPath(appUUID);
    Map<String, String> headers = signer.generateRequestHeaders("GET", requestUrlPath, payload, "");
    String requestUrl = configuration.getBaseUrl() + requestUrlPath;
    return get(requestUrl, headers, publicKeyResponseHandler);
  }

  @Override
  public PublicKey getPublicKey(UUID appUUID) {
    return publicKeyCache.get(appUUID).getPublicKey();
  }

  private String getRequestUrlPath(UUID appUUID) {
    return configuration.getRequestUrlPath() + String.format(configuration.getSecurityTokensUrlPath(), appUUID.toString());
  }

  private <T> T get(String url, Map<String, String> headers, ResponseHandler<T> responseHandler) {
    try {
      HttpGet httpGet = new HttpGet(url);
      for (Entry<String, String> header : headers.entrySet()) {
        httpGet.addHeader(header.getKey(), header.getValue());
      }
      return httpclient.execute(httpGet, responseHandler);
    } catch (IOException ex) {
      logger.error("Public key retrieval error", ex);
      throw new HttpClientPublicKeyProviderException(ex);
    }
  }

  private class PublicKeyResponseHandler implements ResponseHandler<PublicKeyData> {
    private static final String MAX_AGE = "max-age";
    private static final String PUBLIC_KEY_STR = "public_key_str";

    @Override
    public PublicKeyData handleResponse(HttpResponse response) throws IOException {
      if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
        Long timeToLive = getMaxAge(response).orElse(configuration.getTimeToLive());

        HttpEntity entity = response.getEntity();
        String responseAsString = EntityUtils.toString(entity, StandardCharsets.UTF_8);
        ObjectMapper mapper = new ObjectMapper();
        String publicKeyString = mapper.readTree(responseAsString).findValue(PUBLIC_KEY_STR).asText();

        return new PublicKeyData(MAuthKeysHelper.getPublicKeyFromString(publicKeyString), timeToLive);
      } else {
        throw new HttpClientPublicKeyProviderException("Invalid response code returned by server: "
          + response.getStatusLine().getStatusCode());
      }
    }

    public Optional<Long> getMaxAge(HttpResponse response) {
      return Optional.ofNullable(response.getHeaders(HttpHeaders.CACHE_CONTROL))
        .flatMap(headers -> Arrays.stream(headers)
          .flatMap(header -> Arrays.stream(header.getElements())
            .filter(e -> e.getName().equalsIgnoreCase(MAX_AGE))
            .map(e -> Long.parseLong(e.getValue())))
          .findFirst());
    }
  }

  private static class PublicKeyData {
    private final PublicKey publicKey;
    private final Long maxAgeSeconds;

    public PublicKeyData(PublicKey publicKey, Long maxAgeSeconds) {
      this.publicKey = publicKey;
      this.maxAgeSeconds = maxAgeSeconds;
    }

    public PublicKey getPublicKey() {
      return publicKey;
    }

    public Long getMaxAgeSeconds() {
      return maxAgeSeconds;
    }
  }

}
