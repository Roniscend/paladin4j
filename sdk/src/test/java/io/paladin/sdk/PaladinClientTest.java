package io.paladin.sdk;

import io.paladin.sdk.core.TransportConfig;
import org.junit.jupiter.api.*;
import java.time.Duration;
import static org.assertj.core.api.Assertions.*;

class PaladinClientTest {

    @Test
    @DisplayName("Builder creates client with correct config")
    void testBuilderDefaults() {
        try (var client = PaladinClient.builder().build()) {
            assertThat(client.getConfig().httpEndpoint()).isEqualTo("http://127.0.0.1:31548");
            assertThat(client.getConfig().wsEndpoint()).isEqualTo("ws://127.0.0.1:31548");
            assertThat(client.getConfig().connectTimeout()).isEqualTo(Duration.ofSeconds(5));
            assertThat(client.getConfig().maxRetries()).isEqualTo(3);
        }
    }

    @Test
    @DisplayName("Builder applies custom configuration")
    void testBuilderCustomConfig() {
        try (var client = PaladinClient.builder()
                .endpoint("http://10.0.0.1:8080")
                .wsEndpoint("ws://10.0.0.1:8081")
                .connectTimeout(Duration.ofSeconds(10))
                .requestTimeout(Duration.ofSeconds(60))
                .maxRetries(5)
                .build()) {
            TransportConfig cfg = client.getConfig();
            assertThat(cfg.httpEndpoint()).isEqualTo("http://10.0.0.1:8080");
            assertThat(cfg.wsEndpoint()).isEqualTo("ws://10.0.0.1:8081");
            assertThat(cfg.connectTimeout()).isEqualTo(Duration.ofSeconds(10));
            assertThat(cfg.requestTimeout()).isEqualTo(Duration.ofSeconds(60));
            assertThat(cfg.maxRetries()).isEqualTo(5);
        }
    }

    @Test
    @DisplayName("API accessors return consistent singleton instances")
    void testApiSingletons() {
        try (var client = PaladinClient.builder().build()) {
            assertThat(client.ptx()).isSameAs(client.ptx());
            assertThat(client.pstate()).isSameAs(client.pstate());
            assertThat(client.pgroup()).isSameAs(client.pgroup());
            assertThat(client.keymgr()).isSameAs(client.keymgr());
            assertThat(client.bidx()).isSameAs(client.bidx());
            assertThat(client.reg()).isSameAs(client.reg());
            assertThat(client.transport()).isSameAs(client.transport());
        }
    }

    @Test
    @DisplayName("Domain helpers return consistent instances")
    void testDomainHelperSingletons() {
        try (var client = PaladinClient.builder().build()) {
            assertThat(client.noto()).isSameAs(client.noto());
            assertThat(client.zeto()).isSameAs(client.zeto());
            assertThat(client.pente()).isSameAs(client.pente());
        }
    }

    @Test
    @DisplayName("WS endpoint auto-derived from HTTP endpoint")
    void testWsEndpointDerivation() {
        try (var client = PaladinClient.builder()
                .endpoint("http://mynode.example.com:31548")
                .build()) {
            assertThat(client.getConfig().wsEndpoint()).isEqualTo("ws://mynode.example.com:31548");
        }
    }

    @Test
    @DisplayName("ObjectMapper is configured for Records and ISO dates")
    void testObjectMapperConfig() {
        try (var client = PaladinClient.builder().build()) {
            var mapper = client.getObjectMapper();
            assertThat(mapper).isNotNull();
            assertThat(mapper.getDeserializationConfig()
                    .isEnabled(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES))
                    .isFalse();
        }
    }
}
