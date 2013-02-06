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
import no.rmz.blobeeproto.api.proto.Rpc.MethodSignature;
import no.rmz.blobeeproto.api.proto.Rpc.RpcControl;
import org.jboss.netty.channel.ChannelHandlerContext;


public final class RemoteExecutionContext {
    private final MethodSignature methodSignature;
    private final long rpcIndex;
    private final RpcPeerHandler peerHandler;
    private final ChannelHandlerContext ctx;
    private final RpcDirection direction;
    final RpcServiceController controller;

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
        this.controller= new RpcServiceControllerImpl(this);
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

    public void sendControlMessage(final RpcControl msg) {
        checkNotNull(msg);
        peerHandler.sendControlMessage(this, msg);
    }

    public void startCancel() {
        controller.startCancel();
    }
}
