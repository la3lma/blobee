package no.rmz.blobee.rpc;

import com.google.protobuf.Message;
import no.rmz.blobeeproto.api.proto.Rpc.MethodSignature;
import org.jboss.netty.channel.ChannelHandlerContext;

public final class RemoteExecutionContext {
    private final MethodSignature methodSignature;
    private final long rpcIndex;
    private final RpcPeerHandler peerHandler;
    private final ChannelHandlerContext ctx;
    private final RpcDirection direction;

    public RemoteExecutionContext(
            final RpcPeerHandler peerHandler,
            final ChannelHandlerContext ctx,
            final MethodSignature methodSignature,
            final long rpcIndex,
            final RpcDirection direction) {
        this.ctx = ctx;
        this.peerHandler = peerHandler;
        this.methodSignature = methodSignature;
        this.rpcIndex = rpcIndex;
        this.direction = direction;
    }

    public RpcDirection getDirection() {
        return direction;
    }

    public MethodSignature getMethodSignature() {
        return methodSignature;
    }

    public long getRpcIndex() {
        return rpcIndex;
    }

    public void returnResult(final  Message result) {
        peerHandler.returnResult(this, result);
    }

    public ChannelHandlerContext getCtx() {
        return ctx;
    }
}
