/**
 * Copyright 2013  Bjørn Remseth (la3lma@gmail.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package no.rmz.blobee.rpc;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.protobuf.Message;
import com.google.protobuf.MessageLite;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import no.rmz.blobee.protobuf.DynamicProtobufDecoder;
import no.rmz.blobeeproto.api.proto.Rpc;
import no.rmz.blobeeproto.api.proto.Rpc.MethodSignature;
import no.rmz.blobeeproto.api.proto.Rpc.RpcControl;
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
    private final RpcClientFactory rcf;

    /**
     * For each ChannelHandlerContext, this map keeps track of the
     * RemoteExecution context being processed.
     */
    private ContextMap contextMap = new ContextMap();

    private HeartbeatMonitor heartbeatMonitor;


    final Map<Channel, Object> lockMap = new WeakHashMap<Channel, Object>();


    protected RpcPeerHandler(
            final DynamicProtobufDecoder protbufDecoder,
            final MethodSignatureResolver clientResolver,
            final RpcExecutionService executionService,
            final RpcClientFactory rcf) {

        this.clientResolver = checkNotNull(clientResolver); // XXXX This is where the work starts
        this.protbufDecoder = checkNotNull(protbufDecoder);
        this.executionService = checkNotNull(executionService);
        this.rcf = checkNotNull(rcf);
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
        // XXX Check that there is no heartbeat monitor there
        //     already, because if it is,  that is an error situation.
        this.heartbeatMonitor = new HeartbeatMonitor(e.getChannel());
        registerChannel(ctx.getChannel());
    }

    // For clients there will only be only client, but for servers
    // there will (paradoxically :-) be many clients.

    private void registerChannel(final Channel channel) {
        checkNotNull(channel);
        }

    // Stopgap solution to ensure that the client we use is
    // actually the one that is associated with the channel in
    // question.   Must be extended asap.
    private RpcClient getRpcChannel(final Channel channel) {
        checkNotNull(channel);
        return rcf.getClientFor(channel);
    }


    private void nextMessageIsControl() {
        protbufDecoder.putNextPrototype(Rpc.RpcControl.getDefaultInstance());
    }

    @Override
    public void messageReceived(
            final ChannelHandlerContext ctx, final MessageEvent e) {

        final Object message = e.getMessage();

        // First send the object to the listener, if we have one.
        // XXX This should probably be removed since it's  only ever used
        //     for testing and debugging.
        synchronized (listenerLock) {
            if (listener != null) {
                listener.receiveMessage(message, ctx);
            }
        }

        // Then parse it the regular way.
        if (message instanceof Rpc.RpcControl) {

            final Rpc.RpcControl msg = (Rpc.RpcControl) e.getMessage();
            final Rpc.MessageType messageType = msg.getMessageType();

            switch (messageType) {
                case HEARTBEAT:
                    processHeartbeatMessage();
                    break;
                case RPC_INVOCATION:
                    processInvocationMessage(msg, ctx);
                    break;
                case RPC_RETURNVALUE:
                    processReturnValueMessage(msg, ctx);
                    break;
                case SHUTDOWN:
                    processChannelShutdownMessage(ctx);
                    break;
                case INVOCATION_FAILED:
                    processInvocationFailedMessage(ctx.getChannel(), msg);
                    break;
                case RPC_CANCEL:
                    processCancelMessage(msg, ctx);
                    break;
                default:
                    nextMessageIsControl();
                    log.warning("Unknown type of control message: " + message);
            }
        } else {
            processPayloadMessage(message, ctx);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        // Close the connection when an exception is raised.
        log.log(Level.WARNING, "Unexpected exception from downstream.", e.getCause());
        e.getChannel().close();
    }

    @Override
    public void channelClosed(
            final ChannelHandlerContext ctx,
            final ChannelStateEvent e) throws Exception {
        checkNotNull(ctx);
        checkNotNull(e);
        log.log(Level.INFO, "Channel closed");
        super.channelClosed(ctx, e);
        rcf.removeClientFor(ctx.getChannel());
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


    // XXX The executor service should also be a resolver
    private MessageLite getPrototypeForParameter(final MethodSignature methodSignature) {
        checkNotNull(methodSignature);
        final Class parameterType = executionService.getParameterType(methodSignature);
        checkNotNull(parameterType);
        return getPrototypeForMessageClass(parameterType);
    }


   private final MethodSignatureResolver clientResolver;  // XXX For the client

    private MessageLite getPrototypeForReturnValue(final MethodSignature methodSignature) {
        checkNotNull(methodSignature);
        return clientResolver.getPrototypeForReturnValue(methodSignature);
    }

    public void sendControlMessage(
            final RemoteExecutionContext context,
            final RpcControl control) {
        final Channel channel = context.getCtx().getChannel();
        WireFactory.getWireForChannel(channel)
                .write(control);
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



    private void processPayloadMessage(final Object message, final ChannelHandlerContext ctx) throws IllegalStateException {
        nextMessageIsControl();
        log.info("Received payload message:  " + message);

        final RemoteExecutionContext dc = contextMap.get(ctx);

        if (dc == null) {
            throw new IllegalStateException("Protocol decoding error 3");
        }

        if (dc.getDirection() == RpcDirection.INVOKING) {
            executionService.execute(dc, ctx, message);
        } else if (dc.getDirection() == RpcDirection.RETURNING) {
            final Message msg = (Message) message;
            getRpcChannel(ctx.getChannel()).returnCall(dc, msg);
        } else {
            throw new IllegalStateException("Unknown RpcDirection = " + dc.getDirection());
        }
        contextMap.remove(ctx);
    }

    private void processCancelMessage(
            final RpcControl msg,
            final ChannelHandlerContext ctx) {
        nextMessageIsControl();
        final long   rpcIndex = msg.getRpcIndex();
        executionService.startCancel(ctx, rpcIndex);
    }

    private void processInvocationFailedMessage(final Channel channel, final RpcControl msg) {
        checkNotNull(channel);
        checkNotNull(msg);
        nextMessageIsControl();
        final String errorMessage = msg.getFailed();
        final long   rpcIndex = msg.getRpcIndex();
        getRpcChannel(channel).failInvocation(rpcIndex, errorMessage);
    }

    private void processChannelShutdownMessage(final ChannelHandlerContext ctx) {
        nextMessageIsControl();
        ctx.getChannel().close();
    }

    private void processReturnValueMessage(
            final RpcControl msg,
            final ChannelHandlerContext ctx) throws IllegalStateException {
        final MethodSignature methodSignature = msg.getMethodSignature();
        final long rpcIndex = msg.getRpcIndex();
        final MessageLite prototypeForReturnValue =
                getPrototypeForReturnValue(methodSignature);
        checkNotNull(prototypeForReturnValue);
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
    }

    private void processInvocationMessage(final RpcControl msg, final ChannelHandlerContext ctx) throws IllegalStateException {
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
    }

    private void processHeartbeatMessage() {
        nextMessageIsControl();
        heartbeatMonitor.receiveHeartbeat();
    }
}
