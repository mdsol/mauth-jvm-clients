# MAuth Signer Using Akka HTTP

This is an implementation of Medidata Authentication Client Signer to sign the Http requests

## Usage

1. Configuration  
    * MAuth uses [Typesafe Config](https://github.com/typesafehub/config).
      Create `application.conf` on your classpath with following content. The v2_only_sign_requests flag can be set to sign outgoing requests with Mauth protocol V2 only, the default is false and the client sign requests with both x-mws-xxxxx and mcc-xxxxx headers
    
            app {
                uuid: "aaaa-bbbbb-ccccc-ddddd-eeeee"
                private_key: "avasdfasdfadf"
            }
      
        **Defaults:**
        If any of the settings are omitted then following default values will be used.
    
            app {
                uuid: ${?APP_MAUTH_UUID}
                private_key: ${?APP_MAUTH_PRIVATE_KEY}
            }
            mauth {
                v2_only_sign_requests: false
            }

2. Sign requests using Akka HttpClient

    * Please see example [com.mdsol.mauth.MauthRequestSignerExample](src/example/scala/com/mdsol/mauth/MauthRequestSignerExample.scala)

