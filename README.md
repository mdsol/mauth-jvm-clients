# Java Client for MAuth

This is a standalone client for Medidata Authentication

# Medidata Authentication

The Medidata authentication process bounds - in this case verifies - an API message against its origin.

Medidata authentication provides a fault tolerant, service-to-service authentication scheme for Medidata and third-party applications that use web services to communicate with each other.

The process and integrity algorithm are based on digital signatures encrypted and decrypted with private/public key pairs.

Medidata's authentication process requires public key management, which is done by way of an API. It provides message integrity and provenance validation by verifying a message sender's signature. Each public key is associated with an application and is used to authenticate message signatures. Each private key is stored by the application signing requests with the private key. 

**NOTE:** Only the signing application has any knowledge of the application's private key.

## Usage
Medidata Authentication has two parts:

  * Client Side - [Client needs to sign each request](mauth-signer-apachehttp/README.md)
  
  * Server Side - [Server authenticates each request](mauth-authenticator-apachehttp/README.md)
