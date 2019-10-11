package com.mdsol.mauth.util;

import com.mdsol.mauth.MAuthVersion;

import java.util.UUID;

public class MAuthHeadersHelper {

  public static final String AUTH_HEADER_DELIMITER = ";";

  public static String createAuthenticationHeaderValue(UUID appUUID, String encryptedSignature) {
    return createAuthenticationHeaderValue(appUUID, encryptedSignature, MAuthVersion.MWS.getValue());
  }

  public static String createAuthenticationHeaderValue(UUID appUUID, String encryptedSignature, String mauthVersion) {
    String authValue = mauthVersion + " " + appUUID.toString() + ":" + encryptedSignature;
    if (mauthVersion.equalsIgnoreCase(MAuthVersion.MWSV2.toString()))
      authValue += AUTH_HEADER_DELIMITER;
    return authValue;
  }

  public static String createTimeHeaderValue(long epochTime) {
    return String.valueOf(epochTime);
  }

  public static String getSignatureFromAuthenticationHeader(String authenticationHeaderValue) {
    String signature = authenticationHeaderValue.split(":")[1];
    if (getMauthVersion(authenticationHeaderValue).equals(MAuthVersion.MWSV2.getValue())) {
      signature = signature.substring(0, signature.lastIndexOf(AUTH_HEADER_DELIMITER));
    }
    return signature;
  }

  public static UUID getAppUUIDFromAuthenticationHeader(String authenticationHeaderValue) {
    String mauthVersion = getMauthVersion(authenticationHeaderValue).concat(" ");
    String appUUIDAsString = authenticationHeaderValue.split(":")[0].substring(mauthVersion.length());
    return UUID.fromString(appUUIDAsString);
  }

  public static long getRequestTimeFromTimeHeader(String timeHeaderValue) {
    return Long.parseLong(timeHeaderValue);
  }

  public static String getMauthVersion(String authenticationHeaderValue) {
    return authenticationHeaderValue.startsWith(MAuthVersion.MWSV2.getValue() + " ") ?
        MAuthVersion.MWSV2.getValue() : MAuthVersion.MWS.getValue();
  }
}
