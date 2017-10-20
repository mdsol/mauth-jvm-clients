package com.mdsol.mauth.util;

import java.util.UUID;

public class MAuthHeadersHelper {

  public static String createAuthenticationHeaderValue(UUID appUUID, String encryptedSignature) {
    return "MWS " + appUUID.toString() + ":" + encryptedSignature;
  }

  public static String createTimeHeaderValue(long epochTime) {
    return String.valueOf(epochTime);
  }

  public static String getSignatureFromAuthenticationHeader(String authenticationHeaderValue) {
    return authenticationHeaderValue.split(":")[1];
  }

  public static UUID getAppUUIDFromAuthenticationHeader(String authenticationHeaderValue) {
    String appUUIDAsString = authenticationHeaderValue.split(":")[0].substring(4);
    return UUID.fromString(appUUIDAsString);
  }

  public static long getRequestTimeFromTimeHeader(String timeHeaderValue) {
    return Long.parseLong(timeHeaderValue);
  }

}
