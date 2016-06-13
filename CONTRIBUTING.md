# Contributing

1. Clone this repo by executing in your workspace. Checkout `develop` branch
        
        $ git clone git@github.com:mdsol/mauth-java-client.git
        $ cd mauth-java-client
        $ git checkout develop


## Continuous Integration Server (Travis)
Travis server is configured via .travis.yml file.  To get environment variable in to the build container

1. Create `travis/env_variables.sh` file with following example content

        #!/usr/bin/env bash

        export REPO_USERNAME=maven_repo_user
        export REPO_PASSWORD=maven_repo_password

1.  Install `travis` cli

        gem install travis

1. Go to root folder of the project

1. Login to travis

        travis login --pro

1. Encrypt `travis/env_variables.sh` file

        travis encrypt-file travis/env_variables.sh --add

1. Move `env_variables.sh.enc` file to `travis/` and delete `travis/env_variables.sh`

        mv env_variables.sh.enc travis/
        rm travis/env_variables.sh

1. Fix paths in .travis.yml to match file paths so it will look as follows and Source the `env_variables.sh` in travis at `before_install` after `openssl` line above, so the section looks like

        before_install:
        - openssl aes-256-cbc -K $encrypted_39d384624f59_key -iv $encrypted_39d384624f59_iv
          -in travis/env_variables.sh.enc -out env_variables.sh -d
        - . env_variables.sh
