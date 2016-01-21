# Java Client for MAuth - version 2016.1.0

This is a standalone MAuth client for use by internal Medidata teams and authorized 3rd parties.

## Usage

1. Create MAuthConfiguration object using its builder:

 ``` java
    MAuthConfiguration mAuthConfiguration = MAuthConfiguration.Builder.get()
        .withAppUUID(UUID.fromString("UUID of your application as in MAuth registry"))
        .withMAuthUrl("mauth-server.example.com")
        .withDefaultMAuthPaths()
        .withPrivateKey("Private key needed for signing the requests")
        .withPublicKey("Public key for the private key you have provided")
        .build();
 ```

2. Instantiate MAuthService using MAuthServiceClient implementation and pass the configuration object you have created in the previous step:

 ``` java
    MAuthService mAuthService = new MAuthServiceClient(configuration);
 ```
MAuthServiceClient is thread-safe. This means you only need to instantiate it once for given configuration.

3. Use MAuthService to create authentication headers for your requests, e.g. (using Apache HttpClient):

 ``` java
    HttpGet request = new HttpGet("http://example.com/resources/1");
    Map<String, String> headers = 
        mAuthService.generateRequestHeaders(request.getMethod(), "/resources/1", null);
    for (Entry<String, String> header : headers.entrySet()) {
        request.addHeader(header.getKey(), header.getValue());
    }
 ```

 or to validate (authenticate) incoming ones, e.g. (using Servlet Filter):
 
 ``` java
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        MAuthRequest request = MAuthRequest.Builder.get()
            .withAuthenticationHeaderValue(
                request.getHeader(MAuthRequest.MAUTH_AUTHENTICATION_HEADER_NAME))
            .withTimeHeaderValue(
                request.getHeader(MAuthRequest.MAUTH_TIME_HEADER_NAME))
            .withHttpMethod(request.getMethod())
            .withResourcePath(request.getServletPath())
            .withMessagePayload(retrieveRequestBody(request))
            .build();
        boolean validated = mAuthService.validate(request);
        if (validated) {
            // validation succeeded, proceed...
        } else {
            // validation failed, respond with 401...
        }
    }
    
    private byte[] retrieveRequestBody(HttpServletRequest request){
        // get request body from the request...
    }
 ```
