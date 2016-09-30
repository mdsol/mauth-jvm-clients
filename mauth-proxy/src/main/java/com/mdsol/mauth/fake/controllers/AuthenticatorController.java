package com.mdsol.mauth.fake.controllers;

import com.mdsol.mauth.fake.services.AuthenticatorService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

@RestController
public class AuthenticatorController {

  @Autowired
  private AuthenticatorService authenticatorService;

  @Autowired
  private RestTemplate restTemplate;

  @RequestMapping(value = "/app_status")
  public String getStatus() {
    return "ok!";
  }

  @RequestMapping(value = "/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
      RequestMethod.DELETE, RequestMethod.OPTIONS, RequestMethod.HEAD})
  public ResponseEntity<String> authenticateAndForward(RequestEntity<String> entity,
      @RequestHeader(value = "forward-url", required = false) String overridenUrlToForward,
      @RequestHeader(value = "content-type", required = false) String contentType) {
    RequestEntity<String> modifiedRequest =
        authenticatorService.createModifiedRequest(entity, overridenUrlToForward, contentType);
    try {
      return restTemplate.exchange(modifiedRequest, String.class);
    } catch (HttpClientErrorException | HttpServerErrorException ex) {
      return new ResponseEntity<String>(ex.getResponseBodyAsString(), ex.getResponseHeaders(),
          ex.getStatusCode());
    }
  }
}
