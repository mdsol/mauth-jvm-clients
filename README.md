# Java Client for MAuth

This is a standalone client for Medidata Authentication

# Medidata Authentication

The Medidata authentication process bounds - in this case verifies - an API message against its origin.

Medidata authentication provides a fault tolerant, service-to-service authentication scheme for Medidata and third-party applications that use web services to communicate with each other.

The process and integrity algorithm are based on digital signatures encrypted and decrypted with private/public key pairs.

Medidata's authentication process requires public key management, which is done by way of an API. It provides message integrity and provenance validation by verifying a message sender's signature. Each public key is associated with an application and is used to authenticate message signatures. Each private key is stored by the application signing requests with the private key. 

**NOTE:** Only the signing application has any knowledge of the application's private key.

## Usage
A Medidata server requires the requests to be signed using Medidata Authentication

  * Client Side - Client needs to sign each request using an implementation of [Signer](modules/mauth-signer/src/main/java/com/mdsol/mauth/Signer.java) interface.

  Implementations provided
    - [akka-http](modules/mauth-signer-akka-http) (asynchronous)
    - [apache-http](modules/mauth-signer-apachehttp) (synchronous)
  
  * Server Side - Server authenticates each request using an implementation of
    - Asynchronous [Authenticator](modules/mauth-authenticator/src/main/java/com/mdsol/mauth/Authenticator.java) and [ClientPublicKeyProvider](modules/mauth-authenticator/src/main/java/com/mdsol/mauth/utils/ClientPublicKeyProvider.java)
      - [apache-http](modules/mauth-authenticator-apachehttp)
    
    - Synchronous [Authenticator](modules/mauth-authenticator/src/main/scala/com/mdsol/mauth/scaladsl/Authenticator.scala) and [ClientPublicKeyProvider](modules/mauth-authenticator/src/main/scala/com/mdsol/mauth/scaladsl/utils/ClientPublicKeyProvider.scala)
      - [akka-http](modules/mauth-authenticator-akka-http)
