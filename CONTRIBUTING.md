# Contributing

1. Clone this repo by executing in your workspace. Checkout `develop` branch
        
        $ git clone git@github.com:mdsol/mauth-java-client.git
        $ cd mauth-java-client
        $ git checkout develop


## Continuous Integration Server (Travis)
Travis server is configured via .travis.yml file.  To get environment variable in to the build container

1.  Install `travis` cli

        gem install travis

1. Go to root folder of the project

1. Login to travis

        travis login --pro

1. Add Environment variable to travis.yml

        travis encrypt REPO_USERNAME=username --add
        travis encrypt REPO_PASSWORD=password --add
