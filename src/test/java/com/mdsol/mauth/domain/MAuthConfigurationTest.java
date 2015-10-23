package com.mdsol.mauth.domain;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.mdsol.mauth.utils.FixturesLoader;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.UUID;

public class MAuthConfigurationTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static final String MAUTH_BASE_URL = "http://localhost:9001";
  private static final String CUSTOM_MAUTH_URL_PATH = "/mauth";
  private static final String CUSTOM_SECURITY_TOKENS_PATH = "/token/%s";

  private static final String DEFAULT_MAUTH_URL_PATH = "/mauth/v1";
  private static final String DEFAULT_SECURITY_TOKENS_PATH = "/security_tokens/%s.json";

  private static final UUID RESOURCE_APP_UUID = UUID.fromString("92a1869e-c80d-4f06-8775-6c4ebb0758e0");
  private static final String PUBLIC_KEY = FixturesLoader.getPublicKey();
  private static final String PRIVATE_KEY = FixturesLoader.getPrivateKey();

  @Test
  public void shouldNotAllowToCreateConfigurationWithoutAppUUID() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Application UUID cannot be null or empty.");

    MAuthConfiguration.Builder.get().withMAuthUrl(MAUTH_BASE_URL)
        .withMAuthRequestUrlPath(CUSTOM_MAUTH_URL_PATH)
        .withSecurityTokensUrlPath(CUSTOM_SECURITY_TOKENS_PATH).withPrivateKey(PRIVATE_KEY)
        .withPublicKey(PUBLIC_KEY).build();
  }

  @Test
  public void shouldNotAllowToCreateConfigurationWithoutPublicKey() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Public key cannot be null or empty.");

    MAuthConfiguration.Builder.get().withMAuthUrl(MAUTH_BASE_URL)
        .withMAuthRequestUrlPath(CUSTOM_MAUTH_URL_PATH)
        .withSecurityTokensUrlPath(CUSTOM_SECURITY_TOKENS_PATH).withPrivateKey(PRIVATE_KEY)
        .withAppUUID(RESOURCE_APP_UUID).build();
  }

  @Test
  public void shouldNotAllowToCreateConfigurationWithoutPrivateKey() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Private key cannot be null or empty.");

    MAuthConfiguration.Builder.get().withMAuthUrl(MAUTH_BASE_URL)
        .withMAuthRequestUrlPath(CUSTOM_MAUTH_URL_PATH)
        .withSecurityTokensUrlPath(CUSTOM_SECURITY_TOKENS_PATH).withAppUUID(RESOURCE_APP_UUID)
        .withPublicKey(PUBLIC_KEY).build();
  }

  @Test
  public void shouldNotAllowToCreateConfigurationWithoutMAuthUrl() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("MAuth url cannot be null or empty.");

    MAuthConfiguration.Builder.get().withMAuthRequestUrlPath(CUSTOM_MAUTH_URL_PATH)
        .withSecurityTokensUrlPath(CUSTOM_SECURITY_TOKENS_PATH).withAppUUID(RESOURCE_APP_UUID)
        .withPrivateKey(PRIVATE_KEY).withPublicKey(PUBLIC_KEY).build();
  }

  @Test
  public void shouldNotAllowToCreateConfigurationWithoutMAuthUrlPath() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("MAuth request url path cannot be null or empty.");

    MAuthConfiguration.Builder.get().withMAuthUrl(MAUTH_BASE_URL)
        .withSecurityTokensUrlPath(CUSTOM_SECURITY_TOKENS_PATH).withAppUUID(RESOURCE_APP_UUID)
        .withPrivateKey(PRIVATE_KEY).withPublicKey(PUBLIC_KEY).build();
  }

  @Test
  public void shouldNotAllowToCreateConfigurationWithoutSecurityTokensUrl() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Security tokens url path cannot be null or empty.");

    MAuthConfiguration.Builder.get().withMAuthUrl(MAUTH_BASE_URL)
        .withMAuthRequestUrlPath(CUSTOM_MAUTH_URL_PATH).withAppUUID(RESOURCE_APP_UUID)
        .withPrivateKey(PRIVATE_KEY).withPublicKey(PUBLIC_KEY).build();
  }

  @Test
  public void shouldCorrectlyCreateConfigurationWithSpecifiedPaths() {
    MAuthConfiguration configuration = MAuthConfiguration.Builder.get().withMAuthUrl(MAUTH_BASE_URL)
        .withMAuthRequestUrlPath(CUSTOM_MAUTH_URL_PATH)
        .withSecurityTokensUrlPath(CUSTOM_SECURITY_TOKENS_PATH).withAppUUID(RESOURCE_APP_UUID)
        .withPrivateKey(PRIVATE_KEY).withPublicKey(PUBLIC_KEY).build();

    assertThat(configuration.getAppUUID(), equalTo(RESOURCE_APP_UUID));
    assertThat(configuration.getMAuthUrl(), equalTo(MAUTH_BASE_URL));
    assertThat(configuration.getMAuthRequestUrlPath(), equalTo(CUSTOM_MAUTH_URL_PATH));
    assertThat(configuration.getSecurityTokensUrlPath(), equalTo(CUSTOM_SECURITY_TOKENS_PATH));
    assertThat(configuration.getPrivateKey(), equalTo(PRIVATE_KEY));
    assertThat(configuration.getPublicKey(), equalTo(PUBLIC_KEY));
  }

  @Test
  public void shouldCorrectlyCreateConfigurationWithDefaultPaths() {
    MAuthConfiguration configuration = MAuthConfiguration.Builder.get().withMAuthUrl(MAUTH_BASE_URL)
        .withDefaultMAuthPaths().withAppUUID(RESOURCE_APP_UUID).withPrivateKey(PRIVATE_KEY)
        .withPublicKey(PUBLIC_KEY).build();

    assertThat(configuration.getAppUUID(), equalTo(RESOURCE_APP_UUID));
    assertThat(configuration.getMAuthUrl(), equalTo(MAUTH_BASE_URL));
    assertThat(configuration.getMAuthRequestUrlPath(), equalTo(DEFAULT_MAUTH_URL_PATH));
    assertThat(configuration.getSecurityTokensUrlPath(), equalTo(DEFAULT_SECURITY_TOKENS_PATH));
    assertThat(configuration.getPrivateKey(), equalTo(PRIVATE_KEY));
    assertThat(configuration.getPublicKey(), equalTo(PUBLIC_KEY));
  }

}