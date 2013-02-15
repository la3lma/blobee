package no.rmz.blobee.rpc.peer;

/**
 * Exception that's thrown  when the RpcPeerHandler experiences
 * disturbing events.
 */
public final class RpcPeerHandlerException extends Exception {

    public RpcPeerHandlerException(final String message) {
        super(message);
    }

    RpcPeerHandlerException(final Exception ex) {
        super(ex);
    }
}
