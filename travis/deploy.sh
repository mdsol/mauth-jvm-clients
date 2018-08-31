#!/usr/bin/env bash
set -ex

setup_git() {
    git config --global user.email "${GIT_USER_EMAIL}"
    git config --global user.name "${GIT_USER_NAME}"
}

decrypt_private_key() {
    openssl aes-256-cbc -K $encrypted_1ba84326c684_key -iv $encrypted_1ba84326c684_iv -in travis/secring.asc.enc -out project/.gnupg/secring.asc -d
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
