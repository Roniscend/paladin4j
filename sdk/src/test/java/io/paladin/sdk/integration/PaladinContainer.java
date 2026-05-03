package io.paladin.sdk.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * Testcontainers wrapper for a Paladin node.
 *
 * <p>Provides a Docker-based Paladin node for end-to-end integration testing.
 * The container exposes the JSON-RPC HTTP endpoint and the WebSocket endpoint,
 * enabling full SDK testing against a real node without any mocking.
 *
 * <p>The Docker image can be overridden via the {@code PALADIN_IMAGE} system property
 * or environment variable, defaulting to {@code ghcr.io/lf-decentralized-trust-labs/paladin:latest}.
 *
 * <p><strong>Usage:</strong></p>
 * <pre>{@code
 * try (PaladinContainer container = new PaladinContainer()) {
 *     container.start();
 *     String endpoint = container.getHttpEndpoint();
 *     PaladinClient client = PaladinClient.builder().endpoint(endpoint).build();
 *     // ... run tests
 * }
 * }</pre>
 */
public class PaladinContainer extends GenericContainer<PaladinContainer> {

    private static final Logger log = LoggerFactory.getLogger(PaladinContainer.class);

    /** Default Paladin Docker image. Override with {@code -DPALADIN_IMAGE=...} */
    private static final String DEFAULT_IMAGE =
            "ghcr.io/lf-decentralized-trust-labs/paladin:latest";

    /** The HTTP JSON-RPC port exposed by Paladin. */
    public static final int HTTP_PORT = 31548;

    /** The WebSocket port exposed by Paladin. */
    public static final int WS_PORT = 31549;

    /**
     * Creates a new Paladin container with the default or overridden image.
     */
    public PaladinContainer() {
        this(resolveImage());
    }

    /**
     * Creates a new Paladin container with a specific Docker image.
     *
     * @param imageName the full Docker image name including tag
     */
    public PaladinContainer(DockerImageName imageName) {
        super(imageName);
        withExposedPorts(HTTP_PORT, WS_PORT);
        withLogConsumer(new Slf4jLogConsumer(log).withPrefix("paladin"));
        waitingFor(Wait.forHttp("/")
                .forPort(HTTP_PORT)
                .forStatusCode(200)
                .withStartupTimeout(Duration.ofMinutes(3)));
        withStartupTimeout(Duration.ofMinutes(3));
    }

    /**
     * Returns the HTTP JSON-RPC endpoint URL for this container.
     *
     * @return the endpoint URL (e.g., "http://localhost:32789")
     */
    public String getHttpEndpoint() {
        return "http://%s:%d".formatted(getHost(), getMappedPort(HTTP_PORT));
    }

    /**
     * Returns the WebSocket endpoint URL for this container.
     *
     * @return the WebSocket endpoint URL (e.g., "ws://localhost:32790")
     */
    public String getWsEndpoint() {
        return "ws://%s:%d".formatted(getHost(), getMappedPort(WS_PORT));
    }

    private static DockerImageName resolveImage() {
        String image = System.getProperty("PALADIN_IMAGE",
                System.getenv().getOrDefault("PALADIN_IMAGE", DEFAULT_IMAGE));
        log.info("Using Paladin Docker image: {}", image);
        return DockerImageName.parse(image);
    }
}
