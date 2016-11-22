# MAuth Signer Using Apache HttpClient

This is an implementation of Medidata Authentication Client Signer to sign the Http requests

## Usage

1. Configuration
  * MAuth uses [Typesafe Config](https://github.com/typesafehub/config). Create `application.conf` on your classpath with following content

        app {
            uuid: "aaaa-bbbbb-ccccc-ddddd-eeeee"
            private_key: "avasdfasdfadf"
        }
  
    **Defaults:**
    If any of the settings are omitted then following default values will be used.

        app {
            uuid: ${?APP_UUID}
            private_key: ${?APP_PRIVATE_KEY}
        }
  * Load Configuration

        SignerConfiguration configuration = new SignerConfiguration(ConfigFactory.load());

1. Sign requests using Apache HttpClient interceptors
        
        final HttpClientRequestSigner httpClientRequestSigner = new HttpClientRequestSigner(configuration);
        final SignerHttpRequestInterceptor signerHttpRequestInterceptor = new SignerHttpRequestInterceptor(httpClientRequestSigner);
        CloseableHttpClient httpClient = HttpClients
            .custom()
            .addInterceptorFirst(signerHttpRequestInterceptor)
            .build();

and then use `httpClient` as normal
  * Manually signing requests

        final HttpClientRequestSigner httpClientRequestSigner = new HttpClientRequestSigner(configuration);
        HttpGet request = new HttpGet("http://example.com/resources/1");
        httpClientRequestSigner.signRequest(request);
