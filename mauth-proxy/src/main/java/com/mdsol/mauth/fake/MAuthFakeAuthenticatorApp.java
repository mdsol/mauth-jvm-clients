package com.mdsol.mauth.fake;

import com.mdsol.mauth.Signer;
import com.mdsol.mauth.apache.HttpClientRequestSigner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.stream.Collectors;

@SpringBootApplication
public class MAuthFakeAuthenticatorApp {

  public static void main(String... args) {
    SpringApplication.run(MAuthFakeAuthenticatorApp.class, args);
  }

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }

  @Bean
  public Signer mAuthService(@Value("${mauth.appUuid}") String appUuid,
                             @Value("${mauth.privateKeyFilePath}") String privateKeyFilePath) throws IOException {
    String privateKey = new String(Files.readAllBytes(Paths.get(privateKeyFilePath)));
    return new HttpClientRequestSigner(
        UUID.fromString(appUuid),
        privateKey
    );
  }

  /*
  // Embedded Tomcat config to support automatic redirection from http to https.
  @Bean
  public EmbeddedServletContainerFactory tomcatEmbeddedServletContainerFactory() {
    final TomcatEmbeddedServletContainerFactory factory =
        new TomcatEmbeddedServletContainerFactory() {
          @Override
          protected void postProcessContext(Context context) {
            SecurityConstraint securityConstraint = new SecurityConstraint();
            securityConstraint.setUserConstraint("CONFIDENTIAL");
            SecurityCollection collection = new SecurityCollection();
            collection.addPattern("/*");§
            securityConstraint.addCollection(collection);
            context.addConstraint(securityConstraint);
          }
        };
    factory.addAdditionalTomcatConnectors(this.createHttpConnector());
    return factory;
  }

  private Connector createHttpConnector() {
    final String protocol = "org.apache.coyote.http11.Http11NioProtocol";
    final Connector connector = new Connector(protocol);
    connector.setScheme("http");
    connector.setPort(8080);
    connector.setRedirectPort(8443);
    return connector;
  }
  */

}
