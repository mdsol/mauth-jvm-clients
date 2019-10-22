# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Added
- Add support for MWSV2 protocol in Java modules

### Changed
- Renamed com.mdsol.mauth.Signer.generateRequestHeaders to com.mdsol.mauth.Signer.generateRequestHeadersV1
- Renamed com.mdsol.mauth.DefaultSigner.generateRequestHeaders to com.mdsol.mauth.DefaultSigner.generateRequestHeadersV1

### Deprecated
- com.mdsol.mauth.Signer.generateRequestHeadersV1
- com.mdsol.mauth.DefaultSigner.generateRequestHeadersV1
- com.mdsol.mauth.util.MAuthSignatureHelper.generateUnencryptedSignature
