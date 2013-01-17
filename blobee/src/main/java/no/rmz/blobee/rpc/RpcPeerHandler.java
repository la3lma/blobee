package no.rmz.blobee.rpc;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.protobuf.Message;
import com.google.protobuf.MessageLite;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import no.rmz.blobee.handler.codec.protobuf.DynamicProtobufDecoder;
import no.rmz.blobeeproto.api.proto.Rpc;
import no.rmz.blobeeproto.api.proto.Rpc.MethodSignature;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

public final class RpcPeerHandler
        extends SimpleChannelUpstreamHandler {

    private static final Logger log =
            Logger.getLogger(RpcPeerHandler.class.getName());
    /**
     * A constant used when sending heartbeats.
     */
    private static final Rpc.RpcControl HEARTBEAT =
            Rpc.RpcControl.newBuilder()
            .setMessageType(Rpc.MessageType.HEARTBEAT)
            .build();
    private final DynamicProtobufDecoder protbufDecoder;
    /**
     * Used to listen in to incoming messages. Intended for debugging purposes.
     */
    private RpcMessageListener listener;
    /**
     * Used to synchronize access to the listener instance.
     */
    private final Object listenerLock = new Object();
    /**
     * A service that is used to actually execute incoming RPC requests.
     */
    private final RpcExecutionService executionService;
    /**
     * A client used to receive requests for RPC invocations, and also to return
     * incoming responses to the callers.
     */
    private final RpcClient rpcClient;
    /**
     * For each ChannelHandlerContext, this map keeps track of the
     * RemoteExecution context being processed.
     */
    private ContextMap contextMap = new ContextMap();

    protected RpcPeerHandler(
            final DynamicProtobufDecoder protbufDecoder,
            final RpcExecutionService executionService,
            final RpcClient rpcClient) {

        this.protbufDecoder = checkNotNull(protbufDecoder);
        this.executionService = checkNotNull(executionService);
        this.rpcClient = checkNotNull(rpcClient);
    }

    public void setListener(final RpcMessageListener listener) {
        synchronized (listenerLock) {
            this.listener = listener;
        }
    }

    @Override
    public void channelConnected(
            final ChannelHandlerContext ctx,
            final ChannelStateEvent e) {
        WireFactory.getWireForChannel(e.getChannel()).write(HEARTBEAT);
    }

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
        // XXX This block of code is waaay to dense for comfort.
        //     must be refactored for increased readability
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

                final RemoteExecutionContext dc = contextMap.get(ctx);
                if (dc != null) {
                    throw new IllegalStateException("Protocol decoding error 1");
                }

                final RemoteExecutionContext rec = new RemoteExecutionContext(this, ctx,
                        methodSignature, rpcIndex, RpcDirection.INVOKING);
                contextMap.put(ctx, rec);
            } else if (messageType == Rpc.MessageType.RPC_RETURNVALUE) {
                final MethodSignature methodSignature = msg.getMethodSignature();
                final long rpcIndex = msg.getRpcIndex();
                final MessageLite prototypeForReturnValue =
                        getPrototypeForReturnValue(methodSignature);
                protbufDecoder.putNextPrototype(prototypeForReturnValue);

                // XXX Perhaps make an abstraction that requires the value to
                //     be null before setting it to anything else than null?
                final RemoteExecutionContext dc = contextMap.get(ctx);
                if (dc != null) {
                    throw new IllegalStateException("Protocol decoding error 1");
                }
                contextMap.put(ctx,
                        new RemoteExecutionContext(this, ctx, methodSignature, rpcIndex,
                        RpcDirection.RETURNING));
            } else if (messageType == Rpc.MessageType.SHUTDOWN) {
                protbufDecoder.putNextPrototype(Rpc.RpcControl.getDefaultInstance());
                ctx.getChannel().close();
            } else {
                log.warning("Unknown type of control message: " + message);
                protbufDecoder.putNextPrototype(Rpc.RpcControl.getDefaultInstance());
            }

        } else {
            final RemoteExecutionContext dc = contextMap.get(ctx);

            if (dc == null) {
                throw new IllegalStateException("Protocol decoding error 3");
            }

            protbufDecoder.putNextPrototype(Rpc.RpcControl.getDefaultInstance());

            if (dc.getDirection() == RpcDirection.INVOKING) {
                executionService.execute(dc, ctx, message);
            } else if (dc.getDirection() == RpcDirection.RETURNING) {
                final Message msg = (Message) message;
                rpcClient.returnCall(dc, msg);
            } else {
                throw new IllegalStateException("Unknown RpcDirection = " + dc.getDirection());
            }

            contextMap.remove(ctx);
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

    private MessageLite getPrototypeForMessageClass(final Class theClass) {
        checkNotNull(theClass);
        try {
            final Method[] methods = theClass.getMethods();
            for (final Method method : methods) {
                if (method.getName().equals("getDefaultInstance")) {
                    final Object foo = method.invoke(null, null);
                    final MessageLite returnValue = (MessageLite) foo;
                    return returnValue;
                }
            }
        }
        catch (Exception ex) {
            throw new RuntimeException(ex); // XXX FIXME
        }
        throw new RuntimeException("Couldn't find getDefaultIntance method for class " + theClass);
    }

    private MessageLite getPrototypeForParameter(final MethodSignature methodSignature) {
        checkNotNull(methodSignature);
        final Class parameterType = executionService.getParameterType(methodSignature);
        checkNotNull(parameterType);
        return getPrototypeForMessageClass(parameterType);
    }

    private MessageLite getPrototypeForReturnValue(final MethodSignature methodSignature) {
        checkNotNull(methodSignature);
        final Class parameterType = executionService.getReturnType(methodSignature);
        checkNotNull(parameterType);
        return getPrototypeForMessageClass(parameterType);
    }

    void returnResult(final RemoteExecutionContext context, final Message result) {

        final Rpc.RpcControl invocationControl =
                Rpc.RpcControl.newBuilder()
                .setMessageType(Rpc.MessageType.RPC_RETURNVALUE)
                .setStat(Rpc.StatusCode.OK)
                .setRpcIndex(context.getRpcIndex())
                .setMethodSignature(context.getMethodSignature())
                .build();

        final Channel channel = context.getCtx().getChannel();

        WireFactory.getWireForChannel(channel)
                .write(invocationControl, result);
    }
    final Map<Channel, Object> lockMap = new WeakHashMap<Channel, Object>();

    private Object getChannelLock(final Channel channel) {
        synchronized (lockMap) {
            if (lockMap.containsKey(channel)) {
                return lockMap.containsKey(channel);
            } else {
                final Object lock = new Object();
                lockMap.put(channel, lock);
                return lock;
            }
        }
    }
}
