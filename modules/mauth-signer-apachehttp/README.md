# MAuth Signer Using Apache HttpClient

This is an implementation of Medidata Authentication Client Signer to sign the Http requests

## Usage

1. Configuration
   * MAuth uses [Typesafe Config](https://github.com/typesafehub/config).
     Create `application.conf` on your classpath with the following content.
   
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
2. Signing Requests 
   * To sign requests using Apache HttpClient interceptors, please see [com.mdsol.mauth.MauthRequestInterceptorSignerExample](src/example/java/com/mdsol/mauth/MauthRequestInterceptorSignerExample.java)
   * To sign requests manually, please see [com.mdsol.mauth.ManualSignerExample](src/example/java/com/mdsol/mauth/ManualSignerExample.java)
