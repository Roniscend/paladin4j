package io.paladin.sdk.model;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.*;

/**
 * Flexible JSON query filter for Paladin list/query endpoints.
 *
 * <p>Supports equality, comparison, set membership, sorting, and pagination
 * operators. Use the {@link Builder} to construct queries fluently:
 *
 * <pre>{@code
 * QueryJSON query = QueryJSON.builder()
 *         .equal("domain", "noto")
 *         .greaterThan("blockNumber", 100)
 *         .sortDesc("blockNumber")
 *         .limit(10)
 *         .build();
 * }</pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record QueryJSON(
        @JsonProperty("eq") List<Map<String, Object>> eq,
        @JsonProperty("neq") List<Map<String, Object>> neq,
        @JsonProperty("gt") List<Map<String, Object>> gt,
        @JsonProperty("gte") List<Map<String, Object>> gte,
        @JsonProperty("lt") List<Map<String, Object>> lt,
        @JsonProperty("lte") List<Map<String, Object>> lte,
        @JsonProperty("in") List<Map<String, Object>> in,
        @JsonProperty("nin") List<Map<String, Object>> nin,
        @JsonProperty("sort") List<String> sort,
        @JsonProperty("limit") Integer limit,
        @JsonProperty("skip") Integer skip
) {
    /** Creates a new {@link Builder} for constructing a {@code QueryJSON}. */
    public static Builder builder() {
        return new Builder();
    }

    /** Fluent builder for constructing {@link QueryJSON} instances. */
    public static final class Builder {
        private final List<Map<String, Object>> eq = new ArrayList<>();
        private final List<Map<String, Object>> neq = new ArrayList<>();
        private final List<Map<String, Object>> gt = new ArrayList<>();
        private final List<Map<String, Object>> gte = new ArrayList<>();
        private final List<Map<String, Object>> lt = new ArrayList<>();
        private final List<Map<String, Object>> lte = new ArrayList<>();
        private final List<Map<String, Object>> in = new ArrayList<>();
        private final List<Map<String, Object>> nin = new ArrayList<>();
        private final List<String> sort = new ArrayList<>();
        private Integer limit;
        private Integer skip;
        private Builder() {}

        /** Adds an equality filter: {@code field == value}. */
        public Builder equal(String field, Object value) {
            eq.add(Map.of(field, value));
            return this;
        }
        /** Adds a not-equal filter: {@code field != value}. */
        public Builder notEqual(String field, Object value) {
            neq.add(Map.of(field, value));
            return this;
        }
        /** Adds a greater-than filter: {@code field > value}. */
        public Builder greaterThan(String field, Object value) {
            gt.add(Map.of(field, value));
            return this;
        }
        /** Adds a greater-than-or-equal filter: {@code field >= value}. */
        public Builder greaterThanOrEqual(String field, Object value) {
            gte.add(Map.of(field, value));
            return this;
        }
        /** Adds a less-than filter: {@code field < value}. */
        public Builder lessThan(String field, Object value) {
            lt.add(Map.of(field, value));
            return this;
        }
        /** Adds a less-than-or-equal filter: {@code field <= value}. */
        public Builder lessThanOrEqual(String field, Object value) {
            lte.add(Map.of(field, value));
            return this;
        }
        /** Adds a set membership filter: {@code field IN (value)}. */
        public Builder in(String field, Object value) {
            in.add(Map.of(field, value));
            return this;
        }
        /** Adds a set exclusion filter: {@code field NOT IN (value)}. */
        public Builder notIn(String field, Object value) {
            nin.add(Map.of(field, value));
            return this;
        }
        /** Adds an ascending sort on the given field. */
        public Builder sortAsc(String field) {
            sort.add(field);
            return this;
        }
        /** Adds a descending sort on the given field (prefixed with {@code -}). */
        public Builder sortDesc(String field) {
            sort.add("-" + field);
            return this;
        }
        /** Sets the maximum number of results to return. */
        public Builder limit(int limit) {
            this.limit = limit;
            return this;
        }
        /** Sets the number of results to skip (for pagination). */
        public Builder skip(int skip) {
            this.skip = skip;
            return this;
        }
        /** Builds the {@link QueryJSON} instance. Null-safe: empty filters are set to null. */
        public QueryJSON build() {
            return new QueryJSON(
                    eq.isEmpty() ? null : eq,
                    neq.isEmpty() ? null : neq,
                    gt.isEmpty() ? null : gt,
                    gte.isEmpty() ? null : gte,
                    lt.isEmpty() ? null : lt,
                    lte.isEmpty() ? null : lte,
                    in.isEmpty() ? null : in,
                    nin.isEmpty() ? null : nin,
                    sort.isEmpty() ? null : sort,
                    limit, skip
            );
        }
    }
}
