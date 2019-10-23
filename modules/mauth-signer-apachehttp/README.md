# MAuth Signer Using Apache HttpClient

This is an implementation of Medidata Authentication Client Signer to sign the Http requests

## Usage

1. Configuration
   * MAuth uses [Typesafe Config](https://github.com/typesafehub/config).
     Create `application.conf` on your classpath with the following content. The disable_v1 flag can be set to sign outgoing requests with Mauth protocol V2 only, the default is false and the client sign requests with both x-mws-xxxxx and mcc-xxxxx headers

           app {
               uuid: "aaaa-bbbbb-ccccc-ddddd-eeeee"
               private_key: "avasdfasdfadf"
           }
           mauth {
               disable_v1: false
           }
     
       **Defaults:**
       If any of the settings are omitted then following default values will be used.
   
           app {
               uuid: ${?APP_MAUTH_UUID}
               private_key: ${?APP_MAUTH_PRIVATE_KEY}
           }
           mauth {
               disable_v1: false
           }

2. Signing Requests 
   * To sign requests using Apache HttpClient interceptors, please see [com.mdsol.mauth.MauthRequestInterceptorSignerExample](src/example/java/com/mdsol/mauth/MauthRequestInterceptorSignerExample.java)
   * To sign requests manually, please see [com.mdsol.mauth.ManualSignerExample](src/example/java/com/mdsol/mauth/ManualSignerExample.java)
