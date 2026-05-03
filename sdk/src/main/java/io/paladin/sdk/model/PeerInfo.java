package io.paladin.sdk.model;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
/** Information about a peer node in the Paladin transport network. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PeerInfo(
        @JsonProperty("id") String id,
        @JsonProperty("name") String name,
        @JsonProperty("endpoint") String endpoint,
        @JsonProperty("connected") boolean connected
) {}
