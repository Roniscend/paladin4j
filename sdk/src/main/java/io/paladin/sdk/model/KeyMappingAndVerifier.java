package io.paladin.sdk.model;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
/** Maps a wallet key handle to its on-chain verifier address and algorithm. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KeyMappingAndVerifier(
        @JsonProperty("verifier") String verifier,
        @JsonProperty("keyHandle") String keyHandle,
        @JsonProperty("algorithm") String algorithm,
        @JsonProperty("verifierType") String verifierType
) {}
