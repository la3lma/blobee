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

package no.rmz.blobee.controllers;

import com.google.protobuf.RpcController;
import no.rmz.blobee.rpc.client.RpcClientImpl;
import no.rmz.blobee.rpc.client.RpcClientSideInvocation;


/**
 * The extension of the RpcControllr we use when implementing
 * the Rpc.
 */
public interface RpcClientController extends RpcController {

    /**
     * The index of the invocation.   This is always associated to
     * a particular RpcClient.
     * @return
     */
    long getIndex();


    /**
     * True if the controller is associated with an RPC invocation
     * that is still ongoing.
     * @return True if the invocation is still ongoing.
     */
    boolean isActive();

    /**
     * Setting the value that is read by isActive.
     * @param active
     */
    void setActive(final boolean active);


    /**
     * Bind the controller to a record keeping track of the client side
     * of an RPC invocation.
     * @param invocation  The invocation that we want the
     *       controller to be associated with.
     */
    void bindToInvocation(final RpcClientSideInvocation invocation);

    /**
     * Associate the client with a client and an index.
     * @param rpcClient
     * @param rpcIndex
     */
    void setClientAndIndex(final RpcClientImpl rpcClient, final long rpcIndex);
}
