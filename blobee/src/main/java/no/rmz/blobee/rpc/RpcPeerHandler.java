package no.rmz.blobee.rpc;

import java.util.logging.Level;
import java.util.logging.Logger;
import no.rmz.blobee.handler.codec.protobuf.DynamicProtobufDecoder;
import no.rmz.blobeeproto.api.proto.Rpc;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import static com.google.common.base.Preconditions.checkNotNull;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;


public final class RpcPeerHandler extends SimpleChannelUpstreamHandler {
    private static final Logger log = Logger.getLogger(RpcPeerHandler.class.getName());
    private static final Rpc.RpcControl HEARTBEAT =
            Rpc.RpcControl.newBuilder().setMessageType(Rpc.MessageType.HEARTBEAT).build();
    private final DynamicProtobufDecoder protbufDecoder;


    /**
     * Used to listen in to incoming messages. Intended for
     * debugging purposes.
     */
    private RpcMessageListener listener;


    /**
     * Used to synchronize access to the listener instance.
     */
    private final Object listenerLock = new Object();

    protected RpcPeerHandler(final DynamicProtobufDecoder protbufDecoder) {
        this.protbufDecoder = checkNotNull(protbufDecoder);
    }

    public void setListener(final RpcMessageListener listener) {
        synchronized (listenerLock) {
            this.listener = listener;
        }
    }

    @Override
    public void channelConnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) {
        e.getChannel().write(HEARTBEAT);
    }

    @Override
    public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) {
        final Object message = e.getMessage();
        // First send the object to the listener, if we have one.
        synchronized (listenerLock) {
            if (listener != null) {
                listener.receiveMessage(message, ctx);
            }
        }
        // Then parse it the regular way.
        if (message instanceof Rpc.RpcControl) {
            final Rpc.RpcControl msg = (Rpc.RpcControl) e.getMessage();
            protbufDecoder.putNextPrototype(Rpc.RpcControl.getDefaultInstance());
        } else {
            throw new RuntimeException("Unknown type of incoming message to server: " + message);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        // Close the connection when an exception is raised.
        log.log(Level.WARNING, "Unexpected exception from downstream.", e.getCause());
        e.getChannel().close();
    }

}
