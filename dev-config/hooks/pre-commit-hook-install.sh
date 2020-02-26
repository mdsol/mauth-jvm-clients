#!/usr/bin/env bash

_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
DIR=$( echo $_DIR | sed 's/\/dev-config\/hooks$//' )

if [[ $(hash scalafmt 2>/dev/null) -ne 0 ]]; then
  echo "$(tput setaf 3)* installing scalafmt library... $(tput sgr 0)"
  brew install --HEAD olafurpg/scalafmt/scalafmt
fi

if [[ -f ${DIR}/.git/hooks/pre-commit ]]; then
  echo "$(tput setaf 1)* ${DIR}/.git/hooks/pre-commit already exists.$(tput sgr 0)"
else
  ln -s ${DIR}/dev-config/hooks/pre-commit-hook.sh ${DIR}/.git/hooks/pre-commit #create a file link
  echo "$(tput setaf 3)* link to ${DIR}/.git/hooks/pre-commit successfully created.$(tput sgr 0)"
fi
