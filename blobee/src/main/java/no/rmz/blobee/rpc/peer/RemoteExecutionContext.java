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

package no.rmz.blobee.rpc.peer;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.protobuf.Message;
import no.rmz.blobee.controllers.RpcServiceController;
import no.rmz.blobee.controllers.RpcServiceControllerImpl;
import no.rmz.blobee.rpc.peer.wireprotocol.OutgoingRpcAdapter;
import no.rmz.blobee.rpc.peer.wireprotocol.WireFactory;
import no.rmz.blobeeproto.api.proto.Rpc.MethodSignature;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;


/**
 * Keeps the state of an invocation on the server side of the
 * connection.  This is a value object that contains enough
 * information to be able to return a result from an execution.
 */
public final class RemoteExecutionContext {
    private final MethodSignature methodSignature;
    private final long rpcIndex;
    private final RpcPeerHandler peerHandler;
    private final ChannelHandlerContext ctx;
    private final RpcDirection direction;
    private final RpcServiceController controller;
    private final OutgoingRpcAdapter wire;


    public RemoteExecutionContext(
            final RpcPeerHandler peerHandler,
            final ChannelHandlerContext ctx,
            final MethodSignature methodSignature,
            final long rpcIndex,
            final RpcDirection direction,
            final boolean multiReturn,
            final boolean noReturn) {
        this.ctx = checkNotNull(ctx);
        this.peerHandler = checkNotNull(peerHandler);
        this.methodSignature = checkNotNull(methodSignature);
        this.rpcIndex = checkNotNull(rpcIndex);
        this.direction = checkNotNull(direction);
        this.controller = new RpcServiceControllerImpl(this, multiReturn, noReturn);

        final Channel channel = this.getCtx().getChannel();
        this.wire = WireFactory.getWireForChannel(channel);
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


    public void returnResult(
            final Message result) {
        final long rpcIndex = getRpcIndex();
        final MethodSignature methodSignature = getMethodSignature();

        wire.returnRpcResult(rpcIndex, methodSignature, result);
    }

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    public void startCancel() {
        controller.startCancel();
    }
}
