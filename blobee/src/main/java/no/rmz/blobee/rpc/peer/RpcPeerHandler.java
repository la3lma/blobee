/**
 * Copyright 2013 Bjørn Remseth (la3lma@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package no.rmz.blobee.rpc.peer;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageLite;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import no.rmz.blobee.protobuf.DynamicProtobufDecoder;
import no.rmz.blobee.rpc.client.MethodSignatureResolver;
import no.rmz.blobee.rpc.client.RpcClient;
import no.rmz.blobee.rpc.client.RpcClientFactory;
import no.rmz.blobee.rpc.peer.wireprotocol.OutgoingRpcWire;
import no.rmz.blobee.rpc.peer.wireprotocol.WireFactory;
import no.rmz.blobee.rpc.server.RpcExecutionService;
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
    private final MethodSignatureResolver clientResolver;

    protected RpcPeerHandler(
            final DynamicProtobufDecoder protbufDecoder,
            final MethodSignatureResolver clientResolver,
            final RpcExecutionService executionService,
            final RpcClientFactory rcf) {

        this.clientResolver = checkNotNull(clientResolver);
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

    public void runListener(final ChannelHandlerContext ctx, Object message) {
        synchronized (listenerLock) {
            if (listener != null) {
                listener.receiveMessage(message, ctx);
            }
        }
    }

    @Override
    public void messageReceived(
            final ChannelHandlerContext ctx,
            final MessageEvent e) {

        final Object object = e.getMessage();
        if (!( object instanceof Message )) {
            throw new RuntimeException("Unknown type of incoming message in "
                    + this
                    + ".  Type of message was "
                    + object.getClass().getName());
        }

        final Message message = (Message) object;

        // Run listener, if any (only for debugging).
        runListener(ctx, message);

        try {
            // Then parse it the regular way.
            if (message instanceof Rpc.RpcControl) {

                final Rpc.RpcControl msg = (Rpc.RpcControl) e.getMessage();
                final Rpc.MessageType messageType = msg.getMessageType();

                switch (messageType) {
                    case HEARTBEAT:
                        processHeartbeatMessage();
                        break;
                    case RPC_INV:
                        processInvocationMessage(msg, ctx);
                        break;
                    case RPC_RET:
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
        } catch (Exception ex) {
            log.log(Level.SEVERE,
                      "Caught exception while handling "
                    + " message, shutting down connection", ex);
             e.getChannel().close();
        }
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final ExceptionEvent e) {
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

    private MessageLite getPrototypeForMessageClass(final Class theClass)
            throws RpcPeerHandlerException {
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
            throw new RpcPeerHandlerException(ex);
        }
        throw new RpcPeerHandlerException("Couldn't find getDefaultIntance method for class " + theClass);
    }

    // XXX The executor service should also be a resolver
    private MessageLite getPrototypeForParameter(final MethodSignature methodSignature) throws RpcPeerHandlerException {
        checkNotNull(methodSignature);
        final Class parameterType = executionService.getParameterType(methodSignature);
        checkNotNull(parameterType);
        return getPrototypeForMessageClass(parameterType);
    }

    private MessageLite getPrototypeForReturnValue(final MethodSignature methodSignature) {
        checkNotNull(methodSignature);
        return clientResolver.getPrototypeForReturnValue(methodSignature);
    }

    public void returnResult(final RemoteExecutionContext context, final Message result) {
        final long rpcIndex = context.getRpcIndex();
        final MethodSignature methodSignature = context.getMethodSignature();
        final Channel channel = context.getCtx().getChannel();
        final OutgoingRpcWire wire = WireFactory.getWireForChannel(channel);

        wire.returnRpcResult(rpcIndex, methodSignature, result);
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

    private void processPayloadMessage(
            final Message message,
            final ChannelHandlerContext ctx) throws IllegalStateException, RpcPeerHandlerException {
        nextMessageIsControl();

        final RemoteExecutionContext dc = contextMap.get(ctx);

        if (dc == null) {
            throw new RpcPeerHandlerException("Protocol decoding error 3");
        }

        if (dc.getDirection() == RpcDirection.INVOKING) {
            executionService.execute(dc, ctx, message);
        } else if (dc.getDirection() == RpcDirection.RETURNING) {
            final Message msg = (Message) message;
            getRpcChannel(ctx.getChannel()).returnCall(dc, msg);
        } else {
            throw new RpcPeerHandlerException("Unknown RpcDirection = " + dc.getDirection());
        }
        contextMap.remove(ctx);
    }

    private void processCancelMessage(
            final RpcControl msg,
            final ChannelHandlerContext ctx) {
        nextMessageIsControl();
        final long rpcIndex = msg.getRpcIndex();
        executionService.startCancel(ctx, rpcIndex);
    }

    private void processInvocationFailedMessage(final Channel channel, final RpcControl msg) {
        checkNotNull(channel);
        checkNotNull(msg);
        nextMessageIsControl();
        final String errorMessage = msg.getFailed();
        final long rpcIndex = msg.getRpcIndex();
        getRpcChannel(channel).failInvocation(rpcIndex, errorMessage);
    }

    private void processChannelShutdownMessage(final ChannelHandlerContext ctx) {
        nextMessageIsControl();
        ctx.getChannel().close();
    }

    private void processReturnValueMessage(
            final RpcControl msg,
            final ChannelHandlerContext ctx) throws RpcPeerHandlerException {

        nextMessageIsControl();
        final MethodSignature methodSignature = msg.getMethodSignature();
        final long rpcIndex = msg.getRpcIndex();
        final MessageLite prototypeForReturnValue =
                getPrototypeForReturnValue(methodSignature);
        checkNotNull(prototypeForReturnValue);

        final RemoteExecutionContext dc =
                new RemoteExecutionContext(this, ctx, methodSignature, rpcIndex,
                RpcDirection.RETURNING);

        final MessageLite payload;
        try {
            payload = prototypeForReturnValue.newBuilderForType().mergeFrom(msg.getPayload()).build();
        }
        catch (InvalidProtocolBufferException ex) {
            throw new RpcPeerHandlerException(ex);
        }
        checkNotNull(payload);

        final Message pld = (Message) payload;
        checkNotNull(dc);

        getRpcChannel(ctx.getChannel()).returnCall(dc, pld);
    }

    private void processInvocationMessage(final RpcControl msg, final ChannelHandlerContext ctx)
            throws RpcPeerHandlerException {
        try {
            nextMessageIsControl();
            final MethodSignature methodSignature = msg.getMethodSignature();
            final long rpcIndex = msg.getRpcIndex();
            final MessageLite prototypeForParameter =
                    getPrototypeForParameter(methodSignature);


            final RemoteExecutionContext rec = new RemoteExecutionContext(this, ctx,
                    methodSignature, rpcIndex, RpcDirection.INVOKING);
            checkNotNull(rec);


            final MessageLite payload =
                    prototypeForParameter.newBuilderForType().mergeFrom(msg.getPayload()).build();
            checkNotNull(payload);
            // XXX Bug here
            final Message pld = (Message) payload;

            checkNotNull(ctx);
            executionService.execute(rec, ctx, pld);
        }
        catch (InvalidProtocolBufferException ex) {
            throw new RpcPeerHandlerException(ex);
        }
    }


    private void processHeartbeatMessage() {
        nextMessageIsControl();
        heartbeatMonitor.receiveHeartbeat();
    }
}
