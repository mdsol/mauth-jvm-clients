# MAuth Signer Using Akka HTTP

This is an implementation of Medidata Authentication Client Signer to sign the Http requests

## Usage

1. Configuration  
    * MAuth uses [Typesafe Config](https://github.com/typesafehub/config).
      Create `application.conf` on your classpath with following content.
    
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
                            
2. Sign requests using Akka HttpClient

    * Please see example [com.mdsol.mauth.MauthRequestSignerExample](src/example/scala/com/mdsol/mauth/MauthRequestSignerExample.scala)

