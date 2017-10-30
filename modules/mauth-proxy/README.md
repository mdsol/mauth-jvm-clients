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

1. Install docker

        brew install docker

1. Build docker image

        sbt docker

1. cd to the root of this repository

        export APP_UUID='<the mauth id of the app you are proxying to>
        export APP_PRIVATE_KEY='<the actual key (not the path) of the app you are proxying to>
        docker run --env APP_UUID --env APP_PRIVATE_KEY -it --rm -p 9090:9090 --name <your_name> mdsol/mauth-proxy
