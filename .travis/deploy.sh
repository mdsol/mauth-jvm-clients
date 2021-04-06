#!/usr/bin/env bash
set -euxo pipefail

git config --global user.email "${GIT_USER_EMAIL}"
git config --global user.name "${GIT_USER_NAME}"
openssl aes-256-cbc -K "$encrypted_ff550e95c537_key" -iv "$encrypted_ff550e95c537_iv" -in .travis/secret-key.asc.enc -out .travis/secret-key.asc -d
echo $PGP_PASSPHRASE | gpg --passphrase-fd 0 --batch --yes --import .travis/secret-key.asc

sbt 'release cross with-defaults' updateChangelogAndPushOnLatestMaster
