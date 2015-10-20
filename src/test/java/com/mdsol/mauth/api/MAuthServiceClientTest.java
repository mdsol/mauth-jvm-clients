package com.mdsol.mauth.api;

import com.mdsol.mauth.domain.MAuthConfiguration;
import com.mdsol.mauth.utils.FixturesLoader;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.UUID;

public class MAuthServiceClientTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private final String PUBLIC_KEY = FixturesLoader.getPublicKey();
  private final String PRIVATE_KEY = FixturesLoader.getPrivateKey();

  @Test
  public void shouldThrowExceptionIfPassedConfigurationIsNull() {
    // Assert & Act
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("MAuth configuration cannot be null.");

    new MAuthServiceClient(null);
  }

  @Test
  public void shouldInitializeWithoutExceptionOnValidConfiguration() {
    // Arrange
    MAuthConfiguration configuration = MAuthConfiguration.Builder.get()
        .withAppUUID(UUID.fromString("92a1869e-c80d-4f06-8775-6c4ebb0758e0"))
        .withMAuthUrl("http://localhost:9001").withMAuthRequestUrlPath("/mauth/v1")
        .withSecurityTokensUrl("/security_tokens/%s.json").withPublicKey(PUBLIC_KEY)
        .withPrivateKey(PRIVATE_KEY).build();

    // Act & Assert
    new MAuthServiceClient(configuration);
  }

}
