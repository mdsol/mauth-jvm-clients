# Mauth Proxy:
The reason for using the mAuth proxy is to enable the use of UI REST clients like 
[Postman](https://www.getpostman.com/postman), [Paw](https://paw.cloud/) or curl to request Medidata resources.

The service appends valid mAuth headers to incoming requests and 
sends the authenticated request to the requested service.

## Running via sbt:
```bash
export APP_MAUTH_UUID="<the mauth id of the app you are proxying to>"
export APP_MAUTH_PRIVATE_KEY="<the actual key (not the path) of the app you are proxying to>"
./runMauthProxyServer.bash
```

## Running via Docker image

Internal Medidata users please follow [internal guide](https://mdsol.jiveon.com/community/research-development/app-dev/platform-development/services/mauth/blog/2018/03/06/use-mauthed-service-with-a-mauth-unaware-client)

### Installing Docker
You need to have Docker installed on your machine in order to use Mauth Proxy. The simplest way to get Docker running 
on a Mac or Windows machine is to follow the instructions at 
[Docker for Mac](https://docs.docker.com/#/docker-for-mac).

You can create a default VM for docker and run it by running the following
commands:

```bash
docker-machine create --driver virtualbox default
docker-machine start
eval $(docker-machine env)
```

After doing this you should be able to type 'docker ps' and get a listing of all of the
docker process running on your machine. Of course, at this point,
that should be an empty list.  But if this runs without error then docker is ready to go..

### Build Docker Image
To build an image use the `docker` task. Simply run `sbt docker` from your prompt or `docker` in the sbt console.

### Running Mauth Proxy
```bash
export APP_MAUTH_UUID="<the mauth id of the app you are proxying to>"
export APP_MAUTH_PRIVATE_KEY="<the actual key (not the path) of the app you are proxying to>"
docker run --env APP_MAUTH_UUID --env APP_MAUTH_PRIVATE_KEY -it --rm -p 9090:9090 mdsol/mauth_proxy
```
