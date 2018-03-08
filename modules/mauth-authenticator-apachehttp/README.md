# MAuth Authenticator Using Apache HttpClient

This is an implementation of Medidata Authentication Client Authenticator to validate the Http requests

## Usage

1. Configuration
  * MAuth uses [Typesafe Config](https://github.com/typesafehub/config).
  Create `application.conf` on your classpath with following content.

        app {
            uuid: "aaaa-bbbbb-ccccc-ddddd-eeeee"
            private_key: "avasdfasdfadf"
        }
                
        mauth {
            base_url: "http://localhost"
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
            MAuthRequest request = new MAuthRequest(
                            request.getHeader(MAuthRequest.MAUTH_AUTHENTICATION_HEADER_NAME),
                            retrieveRequestBody(request),
                            request.getMethod(),
                            request.getHeader(MAuthRequest.MAUTH_TIME_HEADER_NAME),
                            request.getServletPath()
                          )
            if (authenticator.validate(request)) {
                // validation succeeded, proceed...
            } else {
                // validation failed, respond with 401...
            }
        }

        private byte[] retrieveRequestBody(HttpServletRequest request){
            // get request body from the request...
        }

