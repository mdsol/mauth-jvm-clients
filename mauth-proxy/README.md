## Goal:

The goal of this simple service is to append valid mAuth headers to any incoming request and send this (now authenticated) request to the destination service. It acts like man-in-the-middle for sandbox environments (since it uses sandbox mAuth keys), to make the QA work easier with mAuth.

## General use:

You can use `forward-url` header to specify the url to which the requests are forwarded, for example:

* `forward-url:` http://requestb.in/19sn59z1
* `forward-url:` http://dev3-ctms.imedidata.net/some-endpoint

If you don't specify `forward-url` header, the default value for host part of the url is taken from the `application.properties`.

By default the service runs on port 8080. This can be changed in `application.properties` file.

## Disbursement Gateway:

#### Requests to Disbursement Gateway:
Send your requests to `http://cdg-sandbox-authenticator.imedidata.net:8080` instead of `http://cdg-sandbox.imedidata.net`

#### Requests to other services:
Send your requests to `http://cdg-sandbox-authenticator.imedidata.net` and specify `forward-url` header to tell the service where the requests should be forwarded.


