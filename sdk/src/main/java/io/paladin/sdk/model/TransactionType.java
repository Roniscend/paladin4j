package io.paladin.sdk.model;
import com.fasterxml.jackson.annotation.JsonValue;
/** The visibility type of a Paladin transaction: {@link #PRIVATE} or {@link #PUBLIC}. */
public enum TransactionType {
    PRIVATE("private"),
    PUBLIC("public");
    private final String value;
    TransactionType(String value) {
        this.value = value;
    }
    @JsonValue
    public String getValue() {
        return value;
    }
}
