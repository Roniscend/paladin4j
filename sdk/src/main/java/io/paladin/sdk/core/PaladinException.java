package io.paladin.sdk.core;
public class PaladinException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final int rpcErrorCode;
    private final transient Object rpcErrorData;
    public PaladinException(String message) {
        super(message);
        this.rpcErrorCode = 0;
        this.rpcErrorData = null;
    }
    public PaladinException(String message, Throwable cause) {
        super(message, cause);
        this.rpcErrorCode = 0;
        this.rpcErrorData = null;
    }
    public PaladinException(JsonRpcError rpcError) {
        super("JSON-RPC error %d: %s".formatted(rpcError.code(), rpcError.message()));
        this.rpcErrorCode = rpcError.code();
        this.rpcErrorData = rpcError.data();
    }
    public int getRpcErrorCode() {
        return rpcErrorCode;
    }
    public Object getRpcErrorData() {
        return rpcErrorData;
    }
    public boolean isRpcError() {
        return rpcErrorCode != 0;
    }
}
