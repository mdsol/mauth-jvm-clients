# MAuth Signer Using Akka HTTP

This is an implementation of Medidata Authentication Client Signer to sign the Http requests

## Usage

1. Configuration  
    * MAuth uses [Typesafe Config](https://github.com/typesafehub/config).
      Create `application.conf` on your classpath with following content.
    
            app {
                uuid: "aaaa-bbbbb-ccccc-ddddd-eeeee"
                private_key: "avasdfasdfadf"
            }
      
        **Defaults:**
        If any of the settings are omitted then following default values will be used.
    
            app {
                uuid: ${?APP_MAUTH_UUID}
                private_key: ${?APP_MAUTH_PRIVATE_KEY}
            }
                            
2. Sign requests using Akka HttpClient

    * Please see example [com.mdsol.mauth.MauthRequestSignerExample](src/example/scala/com/mdsol/mauth/MauthRequestSignerExample.scala)

3. Sign requests using Akka HttpClient with distributed tracing

        public class ExampleClass extends TraceHttpClient {
        
          val sender = OkHttpSender.create("http://localhost:9411/api/v2/spans")
          val spanReporter = AsyncReporter.create(sender)
          override implicit val tracer: Tracer = Tracing.newBuilder().localServiceName("serviceName")
            .sampler(Sampler.ALWAYS_SAMPLE).spanReporter(spanReporter).build() 
          
          def clientCall(span: Span) = {
            MAuthRequestSigner(configuration).signRequest(UnsignedRequest(httpMethod, URI.create("http://server"), body, headers)) match {
              case Left(e) => Future.failed(e)
              case Right(signedRequest) => HttpRequest(signedRequest, "traceName", span)
            }
          }
        }
