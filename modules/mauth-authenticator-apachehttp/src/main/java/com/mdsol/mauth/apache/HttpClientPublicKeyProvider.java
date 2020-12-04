package com.mdsol.mauth.apache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mdsol.mauth.AuthenticatorConfiguration;
import com.mdsol.mauth.Signer;
import com.mdsol.mauth.exception.HttpClientPublicKeyProviderException;
import com.mdsol.mauth.util.MAuthKeysHelper;
import com.mdsol.mauth.utils.ClientPublicKeyProvider;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
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
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class HttpClientPublicKeyProvider implements ClientPublicKeyProvider {

  private static final Logger logger = LoggerFactory.getLogger(HttpClientPublicKeyProvider.class);

  private final AuthenticatorConfiguration configuration;
  private final Signer signer;
  private final CloseableHttpClient httpclient;
  private final PublicKeyResponseHandler publicKeyResponseHandler;

  private LoadingCache<UUID, PublicKey> publicKeyCache;

  public HttpClientPublicKeyProvider(AuthenticatorConfiguration configuration, Signer signer) {
    this.configuration = configuration;
    this.signer = signer;
    this.httpclient = HttpClients.createDefault();
    this.publicKeyResponseHandler = new PublicKeyResponseHandler();
    setupCache(configuration.getTimeToLive());
  }

  private void setupCache(long timeToLiveInSeconds) {
    publicKeyCache =
        CacheBuilder.newBuilder()
            .expireAfterAccess(timeToLiveInSeconds, TimeUnit.SECONDS)
            .build(new CacheLoader<UUID, PublicKey>() {
              @Override
              public PublicKey load(UUID appUUID) throws Exception {
                return getPublicKeyFromEureka(appUUID);
              }
            });
  }

  private PublicKey getPublicKeyFromEureka(UUID appUUID) {
    byte[] payload = new byte[0];
    String requestUrlPath = getRequestUrlPath(appUUID);
    Map<String, String> headers = signer.generateRequestHeaders("GET", requestUrlPath, payload, "");
    String requestUrl = configuration.getBaseUrl() + requestUrlPath;
    String publicKeyAsString = get(requestUrl, headers, publicKeyResponseHandler);
    return MAuthKeysHelper.getPublicKeyFromString(publicKeyAsString);
  }

  @Override
  public PublicKey getPublicKey(UUID appUUID) {
    try {
      return publicKeyCache.get(appUUID);
    } catch (Exception e) {
      logger.error("Couldn't find public key", e);
      throw new HttpClientPublicKeyProviderException(e);
    }
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
      throw new HttpClientPublicKeyProviderException(ex);
    }
  }

  private class PublicKeyResponseHandler implements ResponseHandler<String> {

    @Override
    public String handleResponse(HttpResponse response) throws IOException {
      if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
        HttpEntity entity = response.getEntity();
        String responseAsString = EntityUtils.toString(entity, StandardCharsets.UTF_8);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(responseAsString).findValue("public_key_str").asText();
      } else {
        throw new HttpClientPublicKeyProviderException("Invalid response code returned by server: "
            + response.getStatusLine().getStatusCode());
      }
    }
  }
}
