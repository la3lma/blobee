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
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import no.rmz.blobeeproto.api.proto.Rpc;
import no.rmz.blobeeproto.api.proto.Rpc.RpcControl;
import org.jboss.netty.channel.Channel;

// XXX The name is now inappropriate, and there should be
//     a class comment.
public final class OutgoingRpcWireImpl implements OutgoingRpcWire {

    private static  final Logger log =
            Logger.getLogger(OutgoingRpcWireImpl.class.getName());

    /**
     * A constant used when sending heartbeats.
     */
    private static final Rpc.RpcControl HEARTBEAT =
            Rpc.RpcControl.newBuilder()
            .setMessageType(Rpc.MessageType.HEARTBEAT).build();

    /**
     * Monitor used to ensure that message sends, both
     * single and double, are not interleaved with other
     * writes.
     */
    private final Object monitor = new Object();

    /**
     * The channel we are sending messages over.
     */
    private final Channel channel;

    public OutgoingRpcWireImpl(final Channel channel) {
        this.channel = checkNotNull(channel);
    }

    // XXX This could be inlined, it isn't really necessary any longer.
    private void write(final Message msg) {
        checkNotNull(msg);
        synchronized (monitor) {
            channel.write(msg);
        }
    }

    private void sendControlMessage(final RpcControl msg) {
        checkNotNull(channel);
        write(msg);
    }


    private ByteString messageToByteString(final Message msg) {
        checkNotNull(msg);
        final ByteArrayOutputStream baos =
                new ByteArrayOutputStream(msg.getSerializedSize());
        try {
            msg.writeTo(baos);
            baos.close();
        } catch (IOException ex) {
            log.log(Level.SEVERE, "Couldn't serialize payload", ex);
        }
        ByteString payload;
        payload = ByteString.copyFrom(baos.toByteArray());
        checkNotNull(payload);
        return payload;
    }


    @Override
    public void sendInvocation(
            final String methodName,
            final String inputType,
            final String outputType,
            final Long rpcIndex,
            final Message rpParameter) {

        checkNotNull(methodName);
        checkNotNull(inputType);
        checkNotNull(outputType);
        checkNotNull(rpParameter);
        checkArgument(rpcIndex >= 0);

        final ByteString payload =  messageToByteString(rpParameter);
        final Rpc.MethodSignature ms = Rpc.MethodSignature.newBuilder()
                .setMethodName(methodName)
                .setInputType(inputType)
                .setOutputType(outputType)
                .build();

        final Rpc.RpcControl rpcInvocationMessage =
                Rpc.RpcControl.newBuilder()
                .setMessageType(Rpc.MessageType.RPC_INV)
                .setRpcIndex(rpcIndex)
                .setMethodSignature(ms)
                .setPayload(payload)
                .build();

        write(rpcInvocationMessage);
    }

    @Override
    public void returnRpcResult(
            final long rpcIndex,
            final Rpc.MethodSignature methodSignature,
            final Message result) {

        final ByteString payload =  messageToByteString(result);
        final Rpc.RpcControl returnValueMessage =
                Rpc.RpcControl.newBuilder()
                .setMessageType(Rpc.MessageType.RPC_RET)
                .setRpcIndex(rpcIndex)
                .setPayload(payload)
                .setMethodSignature(methodSignature)
                .build();
        write(returnValueMessage);
    }



    public void sendHeartbeat() {
        write(HEARTBEAT);
    }

    @Override
    public void sendCancelMessage(final long rpcIndex) {

        final RpcControl cancelMessage =
                Rpc.RpcControl.newBuilder()
                .setMessageType(Rpc.MessageType.RPC_CANCEL)
                .setRpcIndex(rpcIndex)
                .build();

        sendControlMessage(cancelMessage);
    }

    public void sendInvocationFailedMessage(
            final long rpcIndex,
            final String reason) {
        checkNotNull(reason);
        checkArgument(rpcIndex >= 0);

        final RpcControl failedMessage = RpcControl.newBuilder()
                .setMessageType(Rpc.MessageType.INVOCATION_FAILED)
                .setRpcIndex(rpcIndex)
                .setFailed(reason)
                .build();

        sendControlMessage(failedMessage);
    }
}
