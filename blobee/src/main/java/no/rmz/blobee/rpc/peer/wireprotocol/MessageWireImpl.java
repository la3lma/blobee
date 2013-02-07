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
package no.rmz.blobee.rpc.peer.wireprotocol;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.protobuf.Message;
import no.rmz.blobeeproto.api.proto.Rpc;
import org.jboss.netty.channel.Channel;

public final class MessageWireImpl implements MessageWire {

    final Object monitor = new Object();
    private final Channel channel;

    public MessageWireImpl(final Channel channel) {
        this.channel = checkNotNull(channel);
    }

    private void write(final Message msg1, final Message msg2) {
        checkNotNull(msg1);
        checkNotNull(msg2);
        synchronized (monitor) {
            channel.write(msg1);
            channel.write(msg2);
        }
    }

    public void write(final Message msg1) {
        checkNotNull(msg1);
        synchronized (monitor) {
            channel.write(msg1);
        }
    }

    public void sendInvocation(
            final String methodName,
            final String inputType,
            final String outputType,
            final Long rpcIndex,
            final Message request) {

        checkNotNull(methodName);
        checkNotNull(inputType);
        checkNotNull(outputType);
        checkNotNull(request);
        checkArgument(rpcIndex >= 0);

        final Rpc.MethodSignature ms = Rpc.MethodSignature.newBuilder()
                .setMethodName(methodName)
                .setInputType(inputType)
                .setOutputType(outputType)
                .build();

        final Rpc.RpcControl invocationControl =
                Rpc.RpcControl.newBuilder()
                .setMessageType(Rpc.MessageType.RPC_INVOCATION)
                .setRpcIndex(rpcIndex)
                .setMethodSignature(ms)
                .build();

        // Then send the invocation down the wire.
        write(invocationControl, request);
    }

    @Override
    public void returnRpcResult(
            final long rpcIndex,
            final Rpc.MethodSignature methodSignature,
            final Message result) {
        final Rpc.RpcControl invocationControl =
                Rpc.RpcControl.newBuilder()
                .setMessageType(Rpc.MessageType.RPC_RETURNVALUE)
                .setStat(Rpc.StatusCode.OK)
                .setRpcIndex(rpcIndex)
                .setMethodSignature(methodSignature)
                .build();
        write(invocationControl, result);
    }
}
