package com.mdsol.mauth.util;

import com.mdsol.mauth.MAuthVersion;

import java.util.UUID;

public class MAuthHeadersHelper {

  public static final String DAFAULT_MAUTH_VERSION = MAuthVersion.MWS.getValue();

  public static String createAuthenticationHeaderValue(UUID appUUID, String encryptedSignature) {
    return createAuthenticationHeaderValue(appUUID, encryptedSignature, DAFAULT_MAUTH_VERSION);
  }

  public static String createAuthenticationHeaderValue(UUID appUUID, String encryptedSignature, String mauthVersion) {
    return mauthVersion + " " + appUUID.toString() + ":" + encryptedSignature;
  }

  public static String createTimeHeaderValue(long epochTime) {
    return String.valueOf(epochTime);
  }

  public static String getSignatureFromAuthenticationHeader(String authenticationHeaderValue) {
    return authenticationHeaderValue.split(":")[1];
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
