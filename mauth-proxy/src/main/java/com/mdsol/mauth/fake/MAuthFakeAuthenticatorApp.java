package com.mdsol.mauth.fake;

import com.mdsol.mauth.api.MAuthService;
import com.mdsol.mauth.api.MAuthServiceClient;
import com.mdsol.mauth.domain.MAuthConfiguration;

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
  public MAuthService mAuthService(@Value("${mauth.baseUrl}") String mAuthUrl,
      @Value("${mauth.appUuid}") String appUuid,
      @Value("${mauth.privateKeyFilePath}") String privateKeyFilePath,
      @Value("${mauth.publicKeyFilePath}") String publicKeyFilePath) throws IOException {
    String privateKey = loadFileToString(privateKeyFilePath);
    String publicKey = loadFileToString(publicKeyFilePath);
    MAuthConfiguration configuration = MAuthConfiguration.Builder.get().withMAuthUrl(mAuthUrl)
        .withAppUUID(UUID.fromString(appUuid)).withPrivateKey(privateKey).withPublicKey(publicKey)
        .withDefaultMAuthPaths().build();
    return new MAuthServiceClient(configuration);
  }

  private String loadFileToString(String filePath) throws IOException {
    ClassPathResource resource = new ClassPathResource(filePath);
    String fileContent;
    try (BufferedReader br = new BufferedReader(
        new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
      fileContent = br.lines().collect(Collectors.joining(System.lineSeparator()));
    }
    return fileContent;
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
            collection.addPattern("/*");
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
