package io.paladin.sdk.domain;
import com.fasterxml.jackson.databind.JsonNode;
import io.paladin.sdk.api.PgroupApi;
import io.paladin.sdk.api.PtxApi;
import io.paladin.sdk.model.*;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * High-level client for the <strong>Pente</strong> privacy domain.
 *
 * <p>Pente provides EVM-compatible privacy groups where members can deploy and
 * interact with Solidity smart contracts in a private execution context.
 * This client simplifies creating groups, deploying contracts, and invoking
 * functions within a privacy group.
 *
 * @see PtxApi
 * @see PgroupApi
 */
public final class PenteClient {
    private final PtxApi ptx;
    private final PgroupApi pgroup;
    private static final String DOMAIN = "pente";
    private static final Duration DEFAULT_RECEIPT_TIMEOUT = Duration.ofSeconds(30);

    /** Creates a new {@code PenteClient} backed by the given API clients. */
    public PenteClient(PtxApi ptx, PgroupApi pgroup) {
        this.ptx = ptx;
        this.pgroup = pgroup;
    }

    /**
     * Creates a new Pente privacy group with the specified members.
     *
     * @param name    the human-readable group name
     * @param members the identities of group members
     * @return the created privacy group
     */
    public PrivacyGroup createGroup(String name, String... members) {
        return pgroup.createGroup(PrivacyGroupInput.pente(name, members));
    }

    /**
     * Deploys a Solidity contract within a Pente privacy group.
     *
     * @param groupAddress      the privacy group's contract address
     * @param from              the deployer identity
     * @param abi               the contract ABI as JSON
     * @param bytecode          the compiled contract bytecode
     * @param constructorParams constructor arguments
     * @return a future that completes with the deployment receipt
     */
    public CompletableFuture<TransactionReceipt> deployContract(
            String groupAddress, String from, JsonNode abi, String bytecode,
            Map<String, Object> constructorParams) {
        return ptx.sendTransactionAsync(TransactionInput.builder()
                .type(TransactionType.PRIVATE)
                .domain(DOMAIN)
                .from(from)
                .to(groupAddress)
                .function("constructor")
                .data(constructorParams)
                .build())
                .thenCompose(txId -> ptx.waitForReceipt(txId, DEFAULT_RECEIPT_TIMEOUT));
    }

    /**
     * Invokes a function on a contract deployed within a Pente privacy group.
     *
     * @param groupAddress the privacy group's contract address
     * @param from         the caller identity
     * @param function     the function name to invoke
     * @param params       the function parameters
     * @return a future that completes with the transaction receipt
     */
    public CompletableFuture<TransactionReceipt> invoke(
            String groupAddress, String from, String function,
            Map<String, Object> params) {
        return ptx.sendTransactionAsync(TransactionInput.builder()
                .type(TransactionType.PRIVATE)
                .domain(DOMAIN)
                .from(from)
                .to(groupAddress)
                .function(function)
                .data(params)
                .build())
                .thenCompose(txId -> ptx.waitForReceipt(txId, DEFAULT_RECEIPT_TIMEOUT));
    }

    /**
     * Executes a read-only call against a privacy group's contract.
     *
     * @param groupId  the privacy group ID
     * @param callData the call parameters
     * @return the raw JSON result
     */
    public JsonNode call(String groupId, Map<String, Object> callData) {
        return pgroup.call(groupId, callData);
    }
}
