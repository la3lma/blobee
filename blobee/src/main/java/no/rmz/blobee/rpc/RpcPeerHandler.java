package no.rmz.blobee.rpc;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.protobuf.MessageLite;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import no.rmz.blobee.handler.codec.protobuf.DynamicProtobufDecoder;
import no.rmz.blobeeproto.api.proto.Rpc;
import no.rmz.blobeeproto.api.proto.Rpc.MethodSignature;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

public final class RpcPeerHandler extends SimpleChannelUpstreamHandler {

    private static final Logger log = Logger.getLogger(RpcPeerHandler.class.getName());
    private static final Rpc.RpcControl HEARTBEAT =
            Rpc.RpcControl.newBuilder().setMessageType(Rpc.MessageType.HEARTBEAT).build();
    private final DynamicProtobufDecoder protbufDecoder;
    /**
     * Used to listen in to incoming messages. Intended for debugging purposes.
     */
    private RpcMessageListener listener;
    /**
     * Used to synchronize access to the listener instance.
     */
    private final Object listenerLock = new Object();


    private final RpcExecutionService executionService;




    protected RpcPeerHandler(
            final DynamicProtobufDecoder protbufDecoder,
            final RpcExecutionService executionService) {
        this.protbufDecoder = checkNotNull(protbufDecoder);
        this.executionService = checkNotNull(executionService);
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


    public  final static class DecodingContext {
        MethodSignature methodSignature;
        long rpcIndex;

        public DecodingContext(MethodSignature methodSignature, long rpcIndex) {
            this.methodSignature = methodSignature;
            this.rpcIndex = rpcIndex;
        }
    }

    // XXX How about channel locals instead?
    private Map<ChannelHandlerContext, DecodingContext> context =
            new ConcurrentHashMap<ChannelHandlerContext, DecodingContext>();


    @Override
    public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) {
        final Object message = e.getMessage();

        log.info("Received message:  " + message);

        // First send the object to the listener, if we have one.
        synchronized (listenerLock) {
            if (listener != null) {
                listener.receiveMessage(message, ctx);
            }
        }

        // Then parse it the regular way.
        if (message instanceof Rpc.RpcControl) {
            final Rpc.RpcControl msg = (Rpc.RpcControl) e.getMessage();

            final Rpc.MessageType messageType = msg.getMessageType();
            if (messageType == Rpc.MessageType.HEARTBEAT) {
                // XXX Heartbeats are just ignored.
                protbufDecoder.putNextPrototype(Rpc.RpcControl.getDefaultInstance());
            } else if (messageType == Rpc.MessageType.RPC_INVOCATION) {
                final MethodSignature methodSignature = msg.getMethodSignature();
                final long rpcIndex = msg.getRpcIndex();
                final MessageLite prototypeForParameter =
                        getPrototypeForParameter(methodSignature);
                protbufDecoder.putNextPrototype(prototypeForParameter);
                context.put(ctx, new DecodingContext(methodSignature, rpcIndex));
            } else if (messageType == Rpc.MessageType.RPC_RETURNVALUE) {
                // XXX Not done yet
            } else if (messageType == Rpc.MessageType.SHUTDOWN) {
                 protbufDecoder.putNextPrototype(Rpc.RpcControl.getDefaultInstance());
                ctx.getChannel().close();
            } else {
                log.warning("Unknown type of control message: " + message);
                 protbufDecoder.putNextPrototype(Rpc.RpcControl.getDefaultInstance());
            }

        } else {
            final DecodingContext dc = context.get(ctx);
            protbufDecoder.putNextPrototype(Rpc.RpcControl.getDefaultInstance());
            executionService.execute(dc, ctx, message);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        // Close the connection when an exception is raised.
        log.log(Level.WARNING, "Unexpected exception from downstream.", e.getCause());
        e.getChannel().close();
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        log.log(Level.INFO, "Channel closed");
        super.channelClosed(ctx, e);
    }



    private MessageLite getPrototypeForParameter(MethodSignature methodSignature) {

        // XXX This is a very un-dynamic placeholder for something with a very
        //     dynamic intension.
        return Rpc.RpcParam.getDefaultInstance();
    }
}
