package io.paladin.sdk.model;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
/** An indexed on-chain transaction from the Paladin block indexer. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IndexedTransaction(
        @JsonProperty("hash") String hash,
        @JsonProperty("blockNumber") long blockNumber,
        @JsonProperty("transactionIndex") int transactionIndex,
        @JsonProperty("from") String from,
        @JsonProperty("to") String to,
        @JsonProperty("nonce") long nonce,
        @JsonProperty("result") String result
) {}
