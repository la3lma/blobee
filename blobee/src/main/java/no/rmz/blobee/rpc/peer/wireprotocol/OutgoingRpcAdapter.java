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

import com.google.protobuf.Message;
import no.rmz.blobeeproto.api.proto.Rpc;

/**
 * This interface represents the outgoing direction of an RPC
 * connection.   Everything sent through this interface
 * is atomic in the sense that is either all sent as one package
 * or not sent at all.
 */
public interface OutgoingRpcAdapter {

    /**
     * Send a request to invoke a remote procedure.
     * @param methodName The name of the method, this is a fqdn in the
     *                   java type namespace.
     * @param inputType The name of the input parameter. An fqdn in the  java
     *                   type namespace.
     * @param outputTypeThe name of the output parameter. An fqdn in the  java
     *                   type namespace.
     * @param rpcIndex   For this outgoing connection, this is an index
     *                   uniquely identifying the invocation.
     * @param request    The parameter to the remote procedure.
     * @param multiReturn True iff the invocation can return multiple values.
     * @param noReturn    true iff the invocation is expected to return no value.
     */
    void sendInvocation(
            final String methodName,  // XXXX Rpc.MethodSignature
            final String inputType,
            final String outputType,
            final Long rpcIndex,
            final Message request,
            final boolean multiReturn,
            final boolean noReturn);


    /**
     * Send a return value for an invocation.
     * @param rpcIndex The index of this invocation.
     * @param methodSignature The signature of the method being invoked.
     * @param result The actual result being returned.
     * @param multiReturn True iff the invocation can return multiple values, meaning
     *        this return value may not be the last one.
     */
     void returnRpcResult(
            final long rpcIndex,
            final Rpc.MethodSignature methodSignature,  // XXX Redundant!
            final Message result,
            final boolean multiReturn);

     /**
      * Send a heartbeat message so that the peer at the other
      * end of the connection knows that we're alive.
      */
    void sendHeartbeat();

    /**
     * Send a cancel message.  This is always sent by the client
     * side of an invocation and will result in the invocation being
     * flagged as "cancelled".  The serving method will be informed
     * of this through the RpcController interface.
     * @param rpcIndex Index of the cancelled invocation.
     */
    void sendCancelMessage(final long rpcIndex);

    /**
     * Sent by the serving side of a connection can inform the client
     * side that an invocation failed for some reason.
     * @param rpcIndex The index of the failed invocation.
     * @param reason  A human readable string explaing what went wrong.
     */
    void sendInvocationFailedMessage(final long rpcIndex, final String reason);

    /**
     * Terminate a sequence of return values.  After this invocation
     * the receiving client can delete all references to the invocation.
     * @param rpcIndex The invocation that is now terminating.
     */
    public void terminateMultiReturnSequence(long rpcIndex);
}
