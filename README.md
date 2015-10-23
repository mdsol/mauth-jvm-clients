# Java Client for MAuth - version 2015.2.0

This is a standalone MAuth client for use by internal Medidata teams and authorized 3rd parties.

## Usage

1. Create MAuthConfiguration object using its builder:

 ``` java
    MAuthConfiguration configuration = MAuthConfiguration.Builder.get()
        .withAppUUID(UUID.fromString("UUID of your application, registered in MAuth server"))
        .withMAuthUrl("mauth-server.example.com")
        .withDefaultMAuthPaths()
        .withPrivateKey("Private key needed to sign the requests")
        .withPublicKey("Public key for the private key you have provided")
        .build();
 ```

2. Instantiate MAuthService using MAuthServiceClient implementation and pass the configuration object you have created in the previous step:

 ``` java
MAuthService service = new MAuthServiceClient(configuration);
 ```

3. Use MAuthService to sign your requests and to validate (authenticate) incoming ones.