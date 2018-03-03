## Goal:

The goal of this simple service is to append valid mAuth headers to any incoming request and send this (now authenticated) request to the destination service.

## General use:
Using [Typesafe Config](https://github.com/typesafehub/config)
Create `application.conf` file on your classpath with following settings

        proxy {
            port: 9090
            port: ${?MAUTH_PROXY_PORT}
            buffer_size_in_bytes: 524288
            buffer_size_in_bytes: ${?MAUTH_PROXY_BUFFER_SIZE_IN_BYTES}
        }

        app {
            uuid: ${?APP_UUID}
            private_key: ${?APP_PRIVATE_KEY}
        }

## Start the proxy with Docker

The reason for doing this is so you can use UI REST clients such as [Postman](https://www.getpostman.com/postman) or [Paw](https://paw.cloud/) or even just plain old `curl` to hit Medidata resources

### Installing Docker

You need to have Docker installed on your machine in order to use Mauth Proxy. The simplest way to get Docker running 
on a Mac or Windows machine is to follow the instructions at 
<ahref="https://docs.docker.com/#/docker-for-mac"> Docker for Mac </a>.

> Please note that `boot2docker` has been deprecated in favor of
> `Docker for *` which is maintained by the Docker team.

*Note that Mauth Proxy currently cannot be run on native Docker For Mac.
In order to run mauth-proxy you should kill Docker native and start
docker-machine.  See below.*

You can create a default VM for docker by running the following
command, which allocates sufficient space to run mauth-proxy:

`docker-machine create --driver virtualbox default`

Before starting docker-machine you need to make sure that docker
native is not running.  If Native Docker is running you will see an
icon in the System Menubar of a whale with a large crate on its back.
To stop Native Docker click on the Whale and select "Quit" at the
bottom of the menu.

You can start docker-machine by running `docker-machine start`

You will need to stop Native Docker and start docker-machine everytime
you reboot your machine.

Next you need to run `eval $(docker-machine env)` in your terminal in
order to set up the terminal to run docker.  After doing this you
should be able to type 'docker ps' and get a listing of all of the
docker process running on your machine. Of course, at this point,
that should be an empty list.  But if this runs without error then
docker is ready to go.

Please note again that Mauth Proxy does not currently work with Docker
Native.  It is OK to have it installed on your machine, but in order
to use mauth-proxy you need to have docker-machine running and you
should turn off Native Docker.

### Installing and Configuring AWS CLI

We are using <a href="https://aws.amazon.com/ecr/">AWS ECR </a> as our
docker repository so you need to have the AWS CLI installed on your
machine.  Follow the installation instructions <a
href="http://docs.aws.amazon.com/cli/latest/userguide/installing.html">here</a>
to install the AWS CLI. Then you need to configure the CLI with your
AWS credentials. You can do so by entering the following at the
command line:

`aws configure`

When prompted, enter your AWS **GREEN** credentials.  More info can be
found <a
href="http://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html">here</a>.
* AWS Access Key ID - get this by logging into Medistrano/Self Service
  and, if you don't yet have an AWS, you can create one there
* AWS Secret Access Key - This will also be provided when you create
  an AWS account in Medistrano.  If you already have an AWS account
  but don't remember this key, you will have to create a new key by
  pressing the button
* Default region name - you must enter *us-east-1* if you are using
  the script because it is hard-coded to use this region.
* Default output format - You can simply leave this blank

Next you need to set up temporary ECR login credentials by entering
the following at the command line:

``` `aws ecr get-login` ```

Note the backticks!

Also please note the token created by aws ecr get-login is temporary
and if you experience AWS credentials issues while using Mauth Proxy,
you may need to run it again.

If all went well you should now be able to issue `docker pull`
commands referencing docker images in our ECR repository.

### Running Mauth Proxy

        export APP_UUID='<the mauth id of the app you are proxying to>
        export APP_PRIVATE_KEY='<the actual key (not the path) of the app you are proxying to>
        docker pull aws_account_id.dkr.ecr.us-west-2.amazonaws.com/mdsol/mauth_proxy:latest
        docker run --env APP_UUID --env APP_PRIVATE_KEY -it --rm -p 9090:9090 --name <your_name> mdsol/mauth_proxy:latest
