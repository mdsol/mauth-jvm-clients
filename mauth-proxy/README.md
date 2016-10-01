## Goal:

The goal of this simple service is to append valid mAuth headers to any incoming request and send this (now authenticated) request to the destination service.

## General use:
Using [Typesafe Config](https://github.com/typesafehub/config)
Create `application.conf` file on your classpath with following settings
```json
proxy {
  port: 9090
  port: ${?PROXY_PORT}
  buffer_size_in_bytes: 524288
  buffer_size_in_bytes: ${?BUFFER_SIZE_IN_BYTES}
  forward {
    base_url: "http://localhost:8080"
    base_url: ${?FORWARD_BASE_URL}
  }
}

app {
  uuid: ${?APP_UUID}
  private_key_file: ${?APP_PRIVATE_KEY_FILE}
}
```