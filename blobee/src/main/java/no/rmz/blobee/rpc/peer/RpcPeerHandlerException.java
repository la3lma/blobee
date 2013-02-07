package no.rmz.blobee.rpc.peer;

public final class RpcPeerHandlerException extends Exception {

    public RpcPeerHandlerException(String message) {
        super(message);
    }

    RpcPeerHandlerException(Exception ex) {
        super(ex);
    }
}
