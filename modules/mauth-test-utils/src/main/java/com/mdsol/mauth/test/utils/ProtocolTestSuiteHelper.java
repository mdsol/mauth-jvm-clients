package com.mdsol.mauth.test.utils;

import com.mdsol.mauth.test.utils.model.AuthenticationHeader;
import com.mdsol.mauth.test.utils.model.SigningConfig;
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

public class ProtocolTestSuiteHelper {

  final static String TEST_SUITE_RELATIVE_PATH =
      System.getenv("TEST_SUITE_RELATIVE_PATH") != null ? System.getenv("TEST_SUITE_RELATIVE_PATH")
          : "../../../mauth-protocol-test-suite";
  final static String MWSV2_TEST_CASE_PATH = TEST_SUITE_RELATIVE_PATH + "/protocols/MWSV2/";

  public static SigningConfig loadSigningConfig() {
    String configFile = TEST_SUITE_RELATIVE_PATH + "/signing-config.json";
    ObjectMapper objectMapper = new ObjectMapper();
    SigningConfig signingConfig = null;
    try {
      byte[] jsonData = Files.readAllBytes(normalizePath(configFile));
      signingConfig = objectMapper.readValue(jsonData, SigningConfig.class);
      return signingConfig;
    } catch (IOException ex) {
      System.out.println("Unable to load signing config file " + configFile);
    }
    finally {
      return signingConfig;
    }
  }

  public static String getPrivateKey(String keyFile) {
    String filePath = TEST_SUITE_RELATIVE_PATH + keyFile;
    try {
      return new String(Files.readAllBytes(normalizePath(filePath)), Charset.defaultCharset());
    } catch (IOException ex) {
      throw new IllegalStateException("Unable to load the private key.", ex);
    }
  }

  public static String getPublicKey() {
    String filePath = TEST_SUITE_RELATIVE_PATH + "/signing-params/rsa-key-pub";
    try {
       return new String(Files.readAllBytes(normalizePath(filePath)), Charset.defaultCharset());
    } catch (IOException ex) {
      throw new IllegalStateException("Unable to load the public key." + filePath, ex);
    }
  }

  public static byte[] loadTestData(String filePath) {
    filePath = MWSV2_TEST_CASE_PATH + filePath;
    byte[] bytes = new byte[0];
    try {
      bytes = Files.readAllBytes(normalizePath(filePath));
    } catch (IOException ex) {
      System.out.println("Unable to load: " + filePath);
    }
    finally {
      return bytes;
    }
  }

  public static String loadTestDataAsString(String filePath) {
    byte[] bytes = loadTestData(filePath);
    return bytes.length==0 ? "" : new String(bytes, Charset.defaultCharset());
  }

  public static UnsignedRequest loadUnignedRequest(String filePath) {
    UnsignedRequest unsignedRequest = null;
    byte[] jsonData = loadTestData(filePath);
    if (jsonData.length == 0) return null;

    ObjectMapper objectMapper = new ObjectMapper();
    try {
      unsignedRequest = objectMapper.readValue(jsonData, UnsignedRequest.class);
    } catch (IOException ex) {
      System.out.println("Failed to parse the unsigned request " + filePath);
    }
    finally {
      return unsignedRequest;
    }
  }

  public static byte[] getTestRequestBody(UnsignedRequest unsignedRequest, String caseName) {
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

  public static AuthenticationHeader loadAuthenticationHeader(String filePath) {
    byte[] jsonData = loadTestData(filePath);
    if (jsonData.length == 0) return null;

    AuthenticationHeader authenticationHeader = null;
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      authenticationHeader = objectMapper.readValue(jsonData, AuthenticationHeader.class);
    } catch (IOException ex) {
      System.out.println("Failed to parse Authentication Header " + filePath);
    }
    finally {
      return authenticationHeader;
    }
  }

  public static String[] getTestCases() {
    File file = new File(MWSV2_TEST_CASE_PATH);
    String[] directories = file.list(new FilenameFilter() {
      @Override
      public boolean accept(File current, String name) {
        return new File(current, name).isDirectory();
      }
    });
    return directories;
  }

  private static Path normalizePath(String filepath) {
    return Paths.get(filepath).normalize();
  }
}
