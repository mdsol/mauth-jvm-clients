package com.mdsol.mauth.test.utils;

import com.mdsol.mauth.test.utils.model.AuthenticationHeader;
import com.mdsol.mauth.test.utils.model.AuthenticationOnly;
import com.mdsol.mauth.test.utils.model.SigningAuthentication;
import com.mdsol.mauth.test.utils.model.SigningConfig;
import com.mdsol.mauth.test.utils.model.TestCase;
import com.mdsol.mauth.test.utils.model.UnsignedRequest;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ProtocolTestSuiteHelper {

  final static String TEST_SUITE_RELATIVE_PATH = "../../mauth-protocol-test-suite/";
  final static String MWSV2_TEST_CASE_PATH = getFullFilePath("protocols/MWSV2/");

  public final static SigningConfig SIGNING_CONFIG;
  static {
    SigningConfig tmp = null;
    String configFile = getFullFilePath("signing-config.json");
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      byte[] jsonData = Files.readAllBytes(normalizePath(configFile));
      tmp = objectMapper.readValue(jsonData, SigningConfig.class);
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to load the config file " + configFile, ex);
    }
    SIGNING_CONFIG = tmp;
  }

  public final static String PUBLIC_KEY ;
  static {
    String tmp = null;
    String filePath = getFullFilePath("signing-params/rsa-key-pub");
    try {
      tmp = new String(Files.readAllBytes(normalizePath(filePath)), StandardCharsets.UTF_8);
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to load the public key " + filePath, ex);
    }
    PUBLIC_KEY = tmp;
  }

  public final static TestCase[] TEST_SPECIFICATIONS;
  static {
    List<TestCase> testCaseList = new ArrayList<TestCase>();
    String[] cases = retrieveV2TestCases();

    for (String caseName : cases) {
      String caseFile = caseName.concat("/").concat(caseName);
      AuthenticationHeader authHeader = loadAuthenticationHeader(caseFile.concat(".authz"));
      UnsignedRequest unsignedRequest = loadUnsignedRequest(caseFile.concat(".req"));
      byte[] bodyInBytes = getTestRequestBody(unsignedRequest, caseName);
      unsignedRequest.setBodyInBytes(bodyInBytes);

      boolean isAuthenticationOnly = caseName.contains("authentication-only");
      if(isAuthenticationOnly) {
        testCaseList.add(new AuthenticationOnly(caseName, unsignedRequest, authHeader));
      } else {
        String stringToSign = loadTestDataAsString(caseFile.concat(".sts"));
        String signature = loadTestDataAsString(caseFile.concat(".sig"));
        testCaseList.add(new SigningAuthentication(caseName, unsignedRequest, authHeader, stringToSign, signature));
      }
    }
    TEST_SPECIFICATIONS = testCaseList.toArray(new TestCase[0]);
  }

  public static String loadPrivateKey(String keyFile) {
    String privateKey = "";
    Path filePath = normalizePath(getFullFilePath(keyFile));
    try {
      privateKey = new String(Files.readAllBytes(filePath), Charset.defaultCharset());
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to load the private key " + filePath, ex);
    }
    return privateKey;
  }

  private static String[] retrieveV2TestCases() {
    File file = new File(MWSV2_TEST_CASE_PATH);
    String[] directories = file.list(new FilenameFilter() {
      @Override
      public boolean accept(File current, String name) {
        return new File(current, name).isDirectory();
      }
    });
    return directories;
  }

  private static byte[] loadTestData(String filePath) {
    byte[] bytes = {};
    Path path = normalizePath(MWSV2_TEST_CASE_PATH + filePath);
    try {
      bytes = Files.readAllBytes(path);
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to load " + filePath, ex);
     }
    return bytes;
  }

  private static String loadTestDataAsString(String filePath) {
    byte[] bytes = loadTestData(filePath);
    return bytes.length==0 ? "" : new String(bytes, Charset.defaultCharset());
  }

  private static UnsignedRequest loadUnsignedRequest(String filePath) {
    UnsignedRequest unsignedRequest = new UnsignedRequest();
    byte[] jsonData = loadTestData(filePath);
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      unsignedRequest = objectMapper.readValue(jsonData, UnsignedRequest.class);
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to load " + filePath, ex);
    }
    return unsignedRequest;
  }

  private static byte[] getTestRequestBody(UnsignedRequest unsignedRequest, String caseName) {
    byte[] bytes = new byte[0];
    if (unsignedRequest.getBodyFilepath() != null) {
      String bodyFile = caseName.concat("/").concat(unsignedRequest.getBodyFilepath());
      bytes = ProtocolTestSuiteHelper.loadTestData(bodyFile);
    }
    else if (unsignedRequest.getHttpVerb() != null) {
      bytes = unsignedRequest.getBody().getBytes(StandardCharsets.UTF_8);
    }
    return bytes;
  }

  private static AuthenticationHeader loadAuthenticationHeader(String filePath) {
    AuthenticationHeader authenticationHeader = new AuthenticationHeader();
    byte[] jsonData = loadTestData(filePath);
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      authenticationHeader = objectMapper.readValue(jsonData, AuthenticationHeader.class);
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to load " + filePath, ex);
    }
    return authenticationHeader;
  }

  private static Path normalizePath(String filepath) {
    return Paths.get(filepath).normalize();
  }

  private static String getFullFilePath(String filePath) {
    return TEST_SUITE_RELATIVE_PATH + filePath;
  }
}
