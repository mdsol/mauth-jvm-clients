package com.mdsol.mauth.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdsol.mauth.MAuthConfiguration;
import com.mdsol.mauth.Signer;
import com.mdsol.mauth.exception.HttpClientPublicKeyProviderException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import static com.mdsol.mauth.util.MAuthKeysHelper.getPublicKeyFromString;

public class HttpClientPublicKeyProvider implements ClientPublicKeyProvider {

  private final MAuthConfiguration configuration;
  private final Signer signer;
  private final CloseableHttpClient httpclient;
  private final PublicKeyResponseHandler publicKeyResponseHandler;

  public HttpClientPublicKeyProvider(MAuthConfiguration configuration, Signer signer) {
    this.configuration = configuration;
    this.signer = signer;
    this.httpclient = HttpClients.createDefault();
    this.publicKeyResponseHandler = new PublicKeyResponseHandler();
  }

  @Override
  public PublicKey getPublicKey(UUID appUUID) {
    String requestUrlPath = getRequestUrlPath(appUUID);
    Map<String, String> headers = signer.generateRequestHeaders("GET", requestUrlPath, "");
    String requestUrl = configuration.getUrl() + requestUrlPath;
    String publicKeyAsString = get(requestUrl, headers, publicKeyResponseHandler);
    return getPublicKeyFromString(publicKeyAsString);
  }

  private String getRequestUrlPath(UUID appUUID) {
    return configuration.getRequestUrlPath()
        + String.format(configuration.getSecurityTokensUrlPath(), appUUID.toString());
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
