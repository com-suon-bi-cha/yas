package com.yas.customer.config;

import static org.keycloak.OAuth2Constants.CLIENT_CREDENTIALS;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakClientConfig {
    private final KeycloakPropsConfig keycloakPropsConfig;

    public KeycloakClientConfig(KeycloakPropsConfig keycloakPropsConfig) {
        this.keycloakPropsConfig = keycloakPropsConfig;
    }

    @Bean
    public Client keycloakAdminClient() {
        return ClientBuilder.newBuilder()
            .register(new LenientKeycloakJacksonProvider())
            .build();
    }

    @Bean
    public Keycloak keycloak(Client keycloakAdminClient) {
        return KeycloakBuilder.builder()
            .grantType(CLIENT_CREDENTIALS)
            .serverUrl(keycloakPropsConfig.getAuthServerUrl())
            .realm(keycloakPropsConfig.getRealm())
            .clientId(keycloakPropsConfig.getResource())
            .clientSecret(keycloakPropsConfig.getCredentials().getSecret())
            .resteasyClient(keycloakAdminClient)
            .build();
    }
}
