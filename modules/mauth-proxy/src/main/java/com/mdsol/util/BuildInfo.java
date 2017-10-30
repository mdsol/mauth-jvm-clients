package com.mdsol.util;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BuildInfo {

  private final String name;
  private final String version;
  private final String build;

  @JsonProperty(value = "git_commit")
  private final String gitCommit;

  @JsonProperty(value = "git_branch")
  private final String gitBranch;

  public BuildInfo(final String name, final String version, final String build, final String gitCommit, final String gitBranch) {
    this.name = name;
    this.version = version;
    this.build = build;
    this.gitCommit = gitCommit;
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

  public String getGitCommit() {
    return gitCommit;
  }

  public String getGitBranch() {
    return gitBranch;
  }

}