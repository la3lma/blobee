package no.rmz.blobee.rpc.peer;

public final class RpcPeerHandlerException extends Exception {

    public RpcPeerHandlerException(final String message) {
        super(message);
    }

    RpcPeerHandlerException(final Exception ex) {
        super(ex);
    }
}
