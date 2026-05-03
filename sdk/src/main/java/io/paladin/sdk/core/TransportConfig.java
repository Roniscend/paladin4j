package io.paladin.sdk.core;
import java.time.Duration;
import java.util.Objects;
public record TransportConfig(
        String httpEndpoint,
        String wsEndpoint,
        Duration connectTimeout,
        Duration requestTimeout,
        int maxRetries,
        Duration retryBackoff
) {
    public TransportConfig {
        Objects.requireNonNull(httpEndpoint, "httpEndpoint must not be null");
        if (connectTimeout == null) connectTimeout = Duration.ofSeconds(5);
        if (requestTimeout == null) requestTimeout = Duration.ofSeconds(30);
        if (maxRetries < 0) throw new IllegalArgumentException("maxRetries must be >= 0");
        if (retryBackoff == null) retryBackoff = Duration.ofMillis(500);
        if (wsEndpoint == null) {
            // Replace the protocol part only, carefully.
            // http://localhost:31548 -> ws://localhost:31548
            // (Note: In mapped environments like Testcontainers, the user must provide the explicit WS endpoint
            // because the port will be different from the HTTP port).
            wsEndpoint = httpEndpoint.replaceFirst("^http", "ws");
        }
    }
    public static Builder builder() {
        return new Builder();
    }
    public static final class Builder {
        private String httpEndpoint = "http://127.0.0.1:31548";
        private String wsEndpoint;
        private Duration connectTimeout = Duration.ofSeconds(5);
        private Duration requestTimeout = Duration.ofSeconds(30);
        private int maxRetries = 3;
        private Duration retryBackoff = Duration.ofMillis(500);
        private Builder() {}
        public Builder httpEndpoint(String httpEndpoint) {
            this.httpEndpoint = httpEndpoint;
            return this;
        }
        public Builder wsEndpoint(String wsEndpoint) {
            this.wsEndpoint = wsEndpoint;
            return this;
        }
        public Builder connectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }
        public Builder requestTimeout(Duration requestTimeout) {
            this.requestTimeout = requestTimeout;
            return this;
        }
        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }
        public Builder retryBackoff(Duration retryBackoff) {
            this.retryBackoff = retryBackoff;
            return this;
        }
        public TransportConfig build() {
            return new TransportConfig(httpEndpoint, wsEndpoint, connectTimeout,
                    requestTimeout, maxRetries, retryBackoff);
        }
    }
}
