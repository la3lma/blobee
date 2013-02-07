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

public interface OutgoingRpcWire {

    void sendInvocation(
            final String methodName,
            final String inputType,
            final String outputType,
            final Long rpcIndex,
            final Message request);

    public void returnRpcResult(
            final long rpcIndex,
            final Rpc.MethodSignature methodSignature,
            final Message result);

    public void sendHeartbeat();

    public void sendCancelMessage(final long rpcIndex);

    public void sendInvocationFailedMessage(final long rpcIndex, final String reason);
}
