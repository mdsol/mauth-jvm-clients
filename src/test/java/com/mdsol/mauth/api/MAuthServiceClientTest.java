package com.mdsol.mauth.api;

import com.mdsol.mauth.domain.MAuthConfiguration;
import com.mdsol.mauth.utils.FixturesLoader;

import com.typesafe.config.ConfigFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.UUID;

public class MAuthServiceClientTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static final String PUBLIC_KEY = FixturesLoader.getPublicKey();
  private static final String PRIVATE_KEY = FixturesLoader.getPrivateKey();

  @Test
  public void shouldThrowExceptionIfPassedConfigurationIsNull() {
    // Assert & Act
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("MAuth configuration cannot be null.");

    ConfigFactory.empty();

    new MAuthServiceClient((MAuthConfiguration)null);
  }

  @Test
  public void shouldInitializeWithoutExceptionOnValidConfiguration() {
    // Arrange
    MAuthConfiguration configuration = MAuthConfiguration.Builder.get()
        .withAppUUID(UUID.fromString("92a1869e-c80d-4f06-8775-6c4ebb0758e0"))
        .withMAuthUrl("http://localhost:9001").withMAuthRequestUrlPath("/mauth/v1")
        .withSecurityTokensUrlPath("/security_tokens/%s.json").withPublicKey(PUBLIC_KEY)
        .withPrivateKey(PRIVATE_KEY).build();

    // Act & Assert
    new MAuthServiceClient(configuration);
  }

}
