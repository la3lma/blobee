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
import no.rmz.blobee.rpc.peer.RemoteExecutionContext;

public interface RpcClient {

    void cancelInvocation(final long rpcIndex);

    void failInvocation(final long rpcIndex, final String errorMessage);

    RpcChannel newClientRpcChannel();

    RpcController newController();

    void returnCall(final RemoteExecutionContext dc, final Message message);

    RpcClient start();

    void stop();

    RpcClient addProtobuferRpcInterface(final Object instance);

    RpcClient addInterface(final Class serviceInterface);

    MethodSignatureResolver getResolver();

    RpcClient addInvocationListener(final RpcClientSideInvocationListener listener);
}
