package com.mdsol.mauth.services;

import com.mdsol.mauth.domain.MAuthConfiguration;
import com.mdsol.mauth.exceptions.MAuthHttpClientException;
import com.mdsol.mauth.utils.MAuthKeysHelper;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

public class MAuthHttpClient implements MAuthClient {

  private final MAuthConfiguration configuration;
  private final MAuthSigner signer;

  private final PublicKeyResponseHandler publicKeyResponseHandler = new PublicKeyResponseHandler();

  public MAuthHttpClient(MAuthConfiguration configuration, MAuthSigner signer) {
    this.configuration = configuration;
    this.signer = signer;
  }

  @Override
  public PublicKey getPublicKey(UUID appUUID) {
    String requestUrlPath = configuration.getMauthRequestUrlPath()
        + String.format(configuration.getSecurityTokensUrl(), appUUID.toString());
    Map<String, String> headers = signer.generateHeaders("GET", requestUrlPath, "");
    String requestUrl = configuration.getMauthUrl() + requestUrlPath;
    String publicKeyAsString = get(requestUrl, headers, publicKeyResponseHandler);
    PublicKey publicKey = MAuthKeysHelper.getPublicKeyFromString(publicKeyAsString);
    return publicKey;

  }

  public <T> T get(String url, Map<String, String> headers, ResponseHandler<T> responseHandler) {
    try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
      HttpGet httpGet = new HttpGet(url);
      for (Entry<String, String> header : headers.entrySet()) {
        httpGet.addHeader(header.getKey(), header.getValue());
      }
      try {
        return httpclient.execute(httpGet, responseHandler);
      } catch (ParseException | IOException ex) {
        throw new MAuthHttpClientException(ex);
      }
    } catch (IOException ex) {
      throw new MAuthHttpClientException(ex);
    }
  }

  private class PublicKeyResponseHandler implements ResponseHandler<String> {

    @Override
    public String handleResponse(HttpResponse response)
        throws ClientProtocolException, IOException {
      if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
        HttpEntity entity = response.getEntity();
        String responseAsString = EntityUtils.toString(entity, StandardCharsets.UTF_8);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(responseAsString).findValue("public_key_str").getTextValue();
      } else {
        throw new MAuthHttpClientException("Invalid response code returned by server: "
            + response.getStatusLine().getStatusCode());
      }
    }
  }
}
