package no.rmz.blobee.rpc;

/**
 * Enum used to denote the direction an RPC is going.  
 */
public enum RpcDirection {
    /**
     * An RPC call that is returning with a result from a previous
     * invocation.
     */
    RETURNING,

    /**
     * A new RPC call that expects to be first evaluated and then returned.
     */
    INVOKING
}
