package com.mdso.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class BuildInfoService {
  public static final Logger logger = LoggerFactory.getLogger(BuildInfoService.class);
  public static final String UNKNOWN = "unknown";

  private Properties buildProperties;

  public BuildInfoService() {
    buildProperties = new Properties();
    try {
      InputStream is = getClass().getClassLoader().getResourceAsStream("build.properties");
      if (is != null) {
        buildProperties.load(is);
      }
    } catch (IOException ex) {
      logger.error("error", ex);
    }
  }

  public BuildInfo getBuildInfo() {
    return new BuildInfo(
        buildProperties.getProperty("name", UNKNOWN),
        buildProperties.getProperty("version", UNKNOWN),
        buildProperties.getProperty("build", UNKNOWN),
        buildProperties.getProperty("git_revision", UNKNOWN),
        buildProperties.getProperty("git_branch", UNKNOWN)
    );
  }
}