# MAuth Authenticator Using Akka Http

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
            uuid: ${?APP_UUID}
            private_key: ${?APP_PRIVATE_KEY}
        }
                
        mauth {
            base_url: ${?MAUTH_URL}
            request_url: "/mauth/v1"
            token_url: "/security_tokens/%s.json"
            cache {
                time_to_live_seconds: 90
            }
        }

1. To validate (authenticate) incoming requests, e.g. (using Servlet Filter):

        class MyController extends MAuthDirectives{
            final Config typeSafeConfig = ConfigFactory.load();
            SignerConfiguration singerConfiguration = new SignerConfiguration(typeSafeConfig);
            AuthenticatorConfiguration authConfiguration = new AuthenticatorConfiguration(typeSafeConfig);
            
            implicit val publicKeyProvider = MauthPublicKeyProvider(authConfiguration, MAuthRequestSigner(singerConfiguration))
            implicit val timeout: FiniteDuration = 10 seconds
            implicit val requestValidationTimeout: Duration = 10 seconds
            
            def getResource = authenticate {
                get {
                    path("resources") {
                        complete("OK")
                    }
                }
            }
        }
