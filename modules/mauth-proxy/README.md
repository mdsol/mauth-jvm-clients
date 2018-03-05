# Mauth Proxy:

The goal of this simple service is to append valid mAuth headers to any incoming request and send this (now authenticated) request to the destination service.

## Running via code:
Using [Typesafe Config](https://github.com/typesafehub/config)
Create `application.conf` file on your classpath with following settings

        proxy {
            port: 9090
            port: ${?MAUTH_PROXY_PORT}
            buffer_size_in_bytes: 524288
            buffer_size_in_bytes: ${?MAUTH_PROXY_BUFFER_SIZE_IN_BYTES}
        }

        app {
            uuid: ${?APP_MAUTH_UUID}
            private_key: ${?APP_MAUTH_PRIVATE_KEY}
        }

## Running via Docker image

The reason for doing this is so you can use UI REST clients such as [Postman](https://www.getpostman.com/postman) or [Paw](https://paw.cloud/) or even just plain old `curl` to hit Medidata resources

### Installing Docker

You need to have Docker installed on your machine in order to use Mauth Proxy. The simplest way to get Docker running 
on a Mac or Windows machine is to follow the instructions at 
[Docker for Mac](https://docs.docker.com/#/docker-for-mac).

You can create a default VM for docker and run it by running the following
commands:

```
docker-machine create --driver virtualbox default
docker-machine start
eval $(docker-machine env)
```

After doing this you should be able to type 'docker ps' and get a listing of all of the
docker process running on your machine. Of course, at this point,
that should be an empty list.  But if this runs without error then
docker is ready to go..

### Installing and Configuring AWS CLI

We are using [AWS ECR](https://aws.amazon.com/ecr) as our
docker repository so you need to have the AWS CLI installed on your
machine.  Follow the installation instructions 
[here](http://docs.aws.amazon.com/cli/latest/userguide/installing.html)
to install the AWS CLI. Then you need to configure the CLI with your
AWS credentials. You can do so by entering the following at the
command line:

`aws configure`

Next you need to set up temporary ECR login credentials by entering
the following at the command line:

`$(aws ecr get-login)`

Also please note the token created by aws ecr get-login is temporary
and if you experience AWS credentials issues while using Mauth Proxy,
you may need to run it again.

If all went well you should now be able to issue `docker pull`
commands referencing docker images in our ECR repository.

### Running Mauth Proxy

        export APP_MAUTH_UUID=<the mauth id of the app you are proxying to>
        export APP_MAUTH_PRIVATE_KEY=<the actual key (not the path) of the app you are proxying to>
        docker pull aws_account_id.dkr.ecr.us-west-2.amazonaws.com/mdsol/mauth_proxy:latest
        docker run --env APP_MAUTH_UUID --env APP_MAUTH_PRIVATE_KEY -it --rm -p 9090:9090 mdsol/mauth_proxy:latest
