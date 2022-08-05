# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Changed
- Fix caching to properly re-fetch from MAuth Service in MAuth Authenticator Apachehttp

## [9.0.0] - 2022-07-29

## [8.0.1] - 2022-07-06

## [8.0.0] - 2022-03-23

- Swap scalacache-guava to scalacache-caffeine
- Add an IO interface for ClientPublicKeyProvider

## [7.0.1] - 2021-09-28
### Changed
Upgraded scala cats lib, and fixed resulting breakages. Also updated sbt plugins.

## [7.0.0] - 2021-08-19
### Changed
- Cache responses for 5 minutes (non configurable) as a fallback when the Cache-Control header is missing or malformed.

## [6.0.1] - 2021-07-21
### Added
- parsing code to test with mauth-protocol-test-suite.
- Caffeine dependency for local cache

### Changed
- Dependency update
  - Update sttp to 3.x from 2.x. Note this is a new major sttp version with new package prefix `sttp.client3`
  - Remove silencer as 2.12.13 also got configurable warnings now
- Use Caffeine as local cache instead of Guava Cache
- Added Cache-Control header in FakeMauthServer success response
- Use Cache-Control max-age header as ttl in public key local cache and only set configuration value as fallback

## Removed
- Removed mauth-proxy. The library we depend on (littleproxy) has been unmaintained for a long time
  and there are better mauth proxy alternatives like https://github.com/mdsol/go-mauth-proxy

## [6.0.0] - 2020-12-04
### Added
- Accept request payload as java.io.InputStream for Java. Since InputStream in general can only be consumed once, here are limitations of using stream payload:
  - mauth-signer generates the signature for v2 only even the both v1 and v2 are required.
  - mauth-authenticator doesn't support "Fall back to V1 authentication when V2 authentication fails".

## [5.0.2] - 2020-11-05
### Changed
- Update library versions
### Fixed
- convert `TestFixtures` to java class, was causing cross-compilation problems 

## [5.0.1] - 2020-08-07
### Changed
- Change the default signing versions to 'v1' only

## [5.0.0] - 2020-07-14
### Changed
- Replace `V2_ONLY_SIGN_REQUESTS` option with `MAUTH_SIGN_VERSIONS` option and set the default to `v2` only

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
