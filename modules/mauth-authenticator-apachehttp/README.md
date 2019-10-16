# MAuth Authenticator Using Apache HttpClient

This is an implementation of Medidata Authentication Client Authenticator to validate the Http requests

## Usage

1. Configuration
  * MAuth uses [Typesafe Config](https://github.com/typesafehub/config).
  Create `application.conf` on your classpath with following content. The disable_v1 flag can be set to authenticate incoming requests with Mauth protocol V2 only, default to false.

        app {
            uuid: "aaaa-bbbbb-ccccc-ddddd-eeeee"
            private_key: "avasdfasdfadf"
        }
                
        mauth {
            base_url: "http://localhost"
            disable_v1: false
        }

    **Defaults:**
    If any of the settings are omitted then following default values will be used.

        app {
            uuid: ${?APP_MAUTH_UUID}
            private_key: ${?APP_MAUTH_PRIVATE_KEY}
        }
                
        mauth {
            base_url: ${?MAUTH_URL}
            request_url: "/mauth/v1"
            token_url: "/security_tokens/%s.json"
            cache {
                time_to_live_seconds: 90
            }
            disable_v1: false
        }

  * Load Configuration

        final Config typeSafeConfig = ConfigFactory.load();
        SignerConfiguration singerConfiguration = new SignerConfiguration(typeSafeConfig);
        AuthenticatorConfiguration authConfiguration = new AuthenticatorConfiguration(typeSafeConfig);

1. To validate (authenticate) incoming requests, e.g. (using Servlet Filter):

        HttpClientRequestSigner signer = new HttpClientRequestSigner(signerConfiguration);
        HttpClientPublicKeyProvider provider = new HttpClientPublicKeyProvider(authConfiguration, signer);
        RequestAuthenticator authenticator = new RequestAuthenticator(authenticator);
        
        @Override
        public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
            HttpServletRequest request = (HttpServletRequest) req;
            MAuthVersion mVersion = getMauthVersion(request);
            MAuthRequest mRequest = MAuthRequest.Builder.get()
                .withAuthenticationHeaderValue(request.getHeader(getAuthHeaderName(mVersion)))
                .withTimeHeaderValue(request.getHeader(getTimeHeaderName(mVersion)))
                .withHttpMethod(request.getMethod()).withResourcePath(request.getRequestURI())
                .withQueryParameters(request.getQueryString())
                .withMessagePayload(retrieveRequestBody(request))
                .build();
            if (authenticator.validate(mRequest)) {
                // validation succeeded, proceed...
            } else {
                // validation failed, respond with 401...
            }
        }

        private byte[] retrieveRequestBody(HttpServletRequest request){
            // get request body from the request...
        }

        private MAuthVersion getMauthVersion(HttpServletRequest request) {
          // get the newest mauth version from the request header...
          if (request.getHeader(MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME) != null) {
            return MAuthVersion.MWSV2;
          }
          return MAuthVersion.MWS;
        }

        private String getAuthHeaderName(MAuthVersion version) {
          return version.equals(MAuthVersion.MWSV2) ?
              MAuthRequest.MCC_AUTHENTICATION_HEADER_NAME : MAuthRequest.X_MWS_AUTHENTICATION_HEADER_NAME;
        }

        private String getTimeHeaderName(MAuthVersion version) {
          return version.equals(MAuthVersion.MWSV2) ?
              MAuthRequest.MCC_TIME_HEADER_NAME : MAuthRequest.X_MWS_TIME_HEADER_NAME;
       }
