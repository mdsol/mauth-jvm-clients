package com.mdsol.mauth;

import com.mdsol.mauth.apache.HttpClientRequestSigner;
import com.mdsol.mauth.apache.SignerHttpRequestInterceptor;
import com.typesafe.config.ConfigFactory;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;

public class MauthRequestInterceptorSignerExample {

    private void executeMe() {
        SignerConfiguration configuration = new SignerConfiguration(ConfigFactory.load());
        final HttpClientRequestSigner httpClientRequestSigner = new HttpClientRequestSigner(configuration);
        final SignerHttpRequestInterceptor signerHttpRequestInterceptor = new SignerHttpRequestInterceptor(httpClientRequestSigner);
        CloseableHttpClient httpClient = HttpClients
                .custom()
                .addInterceptorFirst(signerHttpRequestInterceptor)
                .build();

        HttpGet request = new HttpGet("https://api.mdsol.com/v1/countries");

        try(CloseableHttpResponse response = httpClient.execute(request)) {
            StatusLine status = response.getStatusLine();
            System.out.println("response code: " + status.getStatusCode()  + " (" + status.getReasonPhrase() + ")");
            System.out.println("response: " + response.getEntity().getContent().toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Example: How to use Medidata authentication client to sign HTTP requests
     * Set up the following environment variables:
     * APP_MAUTH_UUID - app uuid
     * APP_MAUTH_PRIVATE_KEY - the application private key itself, not the path
     *
     * @param args - no args expected
     */
    public static void main(String[] args) {
        new MauthRequestInterceptorSignerExample().executeMe();
    }
}
