package com.mdso.util;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BuildInfo {

  private final String name;
  private final String version;
  private final String build;

  @JsonProperty(value = "git_revision")
  private final String gitRevision;

  @JsonProperty(value = "git_branch")
  private final String gitBranch;

  public BuildInfo(final String name, final String version, final String build, final String gitRevision, final String gitBranch) {
    this.name = name;
    this.version = version;
    this.build = build;
    this.gitRevision = gitRevision;
    this.gitBranch = gitBranch;
  }

  public String getName() {
    return name;
  }

  public String getVersion() {
    return version;
  }

  public String getBuild() {
    return build;
  }

  public String getGitRevision() {
    return gitRevision;
  }

  public String getGitBranch() {
    return gitBranch;
  }

}