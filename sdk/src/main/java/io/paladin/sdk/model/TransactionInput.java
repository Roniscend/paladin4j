package io.paladin.sdk.model;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
/** Input parameters for submitting a transaction to the Paladin node. Use {@link Builder} to construct. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TransactionInput(
        @JsonProperty("type") TransactionType type,
        @JsonProperty("domain") String domain,
        @JsonProperty("from") String from,
        @JsonProperty("to") JsonNode to,
        @JsonProperty("data") JsonNode data,
        @JsonProperty("function") String function,
        @JsonProperty("abiReference") String abiReference
) {
    public static Builder builder() {
        return new Builder();
    }
    public static final class Builder {
        private TransactionType type = TransactionType.PRIVATE;
        private String domain;
        private String from;
        private JsonNode to;
        private JsonNode data;
        private String function;
        private String abiReference;
        private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER =
                new com.fasterxml.jackson.databind.ObjectMapper();
        private Builder() {}
        public Builder type(TransactionType type) { this.type = type; return this; }
        public Builder domain(String domain) { this.domain = domain; return this; }
        public Builder from(String from) { this.from = from; return this; }
        public Builder to(String to) { this.to = MAPPER.valueToTree(to); return this; }
        public Builder to(JsonNode to) { this.to = to; return this; }
        public Builder data(Map<String, Object> data) { this.data = MAPPER.valueToTree(data); return this; }
        public Builder data(JsonNode data) { this.data = data; return this; }
        public Builder function(String function) { this.function = function; return this; }
        public Builder abiReference(String abiReference) { this.abiReference = abiReference; return this; }
        public TransactionInput build() {
            return new TransactionInput(type, domain, from, to, data, function, abiReference);
        }
    }
}
