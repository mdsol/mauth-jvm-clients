#!/usr/bin/env bash
set -e

setup_git() {
    git config --global user.email "${GIT_USER_EMAIL}"
    git config --global user.name "${GIT_USER_NAME}"
}

if [[ ${TRAVIS_PULL_REQUEST} == false ]] ; then
    if [[ ${TRAVIS_BRANCH} == master ]] ; then
        setup_git
        git checkout master
        sbt ++${TRAVIS_SCALA_VERSION} 'release with-defaults skip-tests'
    else
        if [[ ${TRAVIS_BRANCH} != develop ]]; then
            sed -i "s/-SNAPSHOT/.${TRAVIS_BUILD_NUMBER}-SNAPSHOT/g" ${TRAVIS_BUILD_DIR}/version.sbt
        fi
        sbt ++${TRAVIS_SCALA_VERSION} publish
    fi
fi
