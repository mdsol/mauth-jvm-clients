#!/usr/bin/env bash
set -ex

setup_git() {
    git config --global user.email "${GIT_USER_EMAIL}"
    git config --global user.name "${GIT_USER_NAME}"
}

decrypt_private_key() {
  openssl aes-256-cbc -K $encrypted_ff550e95c537_key -iv $encrypted_ff550e95c537_iv -in .travis/secret-key.asc.enc -out .travis/secret-key.asc -d
  echo $PGP_PASSPHRASE | gpg --passphrase-fd 0 --batch --yes --import .travis/secret-key.asc
}

if [[ ${TRAVIS_PULL_REQUEST} == false && -z "$TRAVIS_TAG" ]] ; then
  decrypt_private_key
    if [[ ${TRAVIS_BRANCH} == master ]] ; then
        setup_git
        git checkout master
        sbt ++${TRAVIS_SCALA_VERSION} 'release cross with-defaults skip-tests'
    elif [[ ${TRAVIS_BRANCH} == develop ]]; then
        sbt ++${TRAVIS_SCALA_VERSION} +publishSigned
    fi
fi
