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


## Deploying artifacts
Travis CI is setup to deploy artifacts, built jar and sources jar to maven repo after each successful build.  
  
Make sure that the version number has `-SNAPSHOT` in all branches but master as maven repo will only allow one artifact without SNAPSHOT
 
## Releasing
To release

1. Checkout `master`
1. Merge `develop` to `master`
1. Change version number to match release without `-SNAPSHOT`. e.g. `2016.1.1`
1. Push
1. Go to [Releases](https://github.com/mdsol/mauth-java-client/releases) tab on github and tag with the version number
