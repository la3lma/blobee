package no.rmz.blobee.rpc;

import com.google.protobuf.Message;
import no.rmz.blobeeproto.api.proto.Rpc.MethodSignature;
import no.rmz.blobeeproto.api.proto.Rpc.RpcControl;
import org.jboss.netty.channel.ChannelHandlerContext;
import static com.google.common.base.Preconditions.checkNotNull;


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
        this.ctx = checkNotNull(ctx);
        this.peerHandler = checkNotNull(peerHandler);
        this.methodSignature = checkNotNull(methodSignature);
        this.rpcIndex = checkNotNull(rpcIndex);
        this.direction = checkNotNull(direction);
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

    void sendControlMessage(final RpcControl msg) {
        checkNotNull(msg);
        peerHandler.sendControlMessage(this, msg);
    }
}
