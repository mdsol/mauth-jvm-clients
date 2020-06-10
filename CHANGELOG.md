# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [4.0.2] - 2020-06-10

## [4.0.1] - 2020-06-09
### Changed
- Use the encoded resource path and query parameters for Mauth singer
- Fall back to V1 authentication when V2 authentication fails using Akka Http

## [4.0.0] - 2020-03-19
### Added
- scalac flags for 2.12 and 2.13
- Address deprecation warnings, silence
- Use sbt-smartrelease plugin

### Remove
- unused `sbt-mima-plugin`, `scalastyle-sbt-plugin` plugins

## [3.0.0]
### Added
- Add cross compilation for 2.12 and 2.13

## [2.1.0]
### Changed
- Fall back to V1 authentication when V2 authentication fails.

## [2.0.5]
### Added
- Add support for MWSV2 protocol in Java modules
- Add support for MWSV2 protocol in Scala modules

### Deprecated
- All method and helper functions that deal with only MAuth V1 headers has been deprecated. 
  Read the deprecation message of each for how to migrate.
