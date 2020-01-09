package com.mdsol.mauth;

/*
 * Contains the Enumeration values for different versions supported by the library.
 */
public enum MAuthVersion {
  
  // Defines the enumeration value for V1 protocol.
  MWS("MWS"),

  // Defines the enumeration value for V2 protocol.
  MWSV2("MWSV2");
  
  private final String value;

  MAuthVersion(final String value) {
      this.value = value;
  }

  public String getValue() {
      return value;
  }
  
}
