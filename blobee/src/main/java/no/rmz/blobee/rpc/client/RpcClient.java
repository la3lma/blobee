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

package no.rmz.blobee.rpc.client;

import com.google.protobuf.Message;
import com.google.protobuf.RpcChannel;
import com.google.protobuf.RpcController;
import no.rmz.blobee.rpc.methods.MethodSignatureResolver;
import no.rmz.blobee.rpc.peer.RemoteExecutionContext;

/**
 * An implement implementing an RPC client that
 * can be used as an utility function for clients
 * implementing protoc generated rpc interfaces.
 */
public interface RpcClient {

    /**
     * Cancel invocation.
     * @param rpcIndex  Index of the invocation to cancel.
     */
    void cancelInvocation(final long rpcIndex);

    /**
     * Signal that an invocation has failed.
     * @param rpcIndex The index of the invocation to fail.
     * @param errorMessage  A human readable error message.
     */
    void failInvocation(final long rpcIndex, final String errorMessage);

    /**
     * Generate a new client channel to be used for communicating
     * through this RpcClient.
     * @return An RPCChannel that can be used to invoke remote procedures.
     */
    RpcChannel newClientRpcChannel();

    /**
     * Get a new controller associated with this client.
     * @return a new controller
     */
    RpcController newController();

    /**
     * Return the value of an invocation.
     * @param dc The context for this invocation.
     * @param message The value to return.
     */
    void returnCall(final RemoteExecutionContext dc, final Message message);

    /**
     * Start the client.  After the client is started it an be used
     * to handle requests.
     * @return The present RpcClient.
     */
    RpcClient start();

    /**
     * Stop the client from accepting incoming invocations.
     */
    void stop();


    // XXX Don't understand what this does.
    RpcClient addProtobuferRpcInterface(final Object instance);

    /**
     * Let the client handle requests for a service interface.
     * @param serviceInterface A protoc generated service interface.
     * @return The present rpc client.
     */
    RpcClient addInterface(final Class serviceInterface);

    /**
     * Get the resolver instance used by this client to map
     * methods to parameter/return value prototypes.
     * @return the resolver.
     */
    MethodSignatureResolver getResolver();

    /**
     * Used for debugging and testing.  Used to listen in on rpc
     * requests that pass through the client.
     * @param listener a listener
     * @return this rpc client.
     */
    RpcClient addInvocationListener(
            final RpcClientSideInvocationListener listener);
}
