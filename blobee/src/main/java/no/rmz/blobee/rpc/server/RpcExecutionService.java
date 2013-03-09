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
package no.rmz.blobee.rpc.server;

import com.google.protobuf.Message;
import no.rmz.blobee.rpc.peer.RemoteExecutionContext;
import no.rmz.blobeeproto.api.proto.Rpc;
import org.jboss.netty.channel.ChannelHandlerContext;

/**
 * Execute incoming requests for remote procedure executions.
 */
public interface RpcExecutionService {

    /// XXX Does this method need to be in this interface?
    /**
     * Look Find the return type associated with a method signature.
     *
     * @param sig The signature
     * @return The return type of a method signature.
     */
    Class getReturnType(final Rpc.MethodSignature sig);

    /// XXX Does this method need to be in this interface?

    /**
     * Look Find the parameter type associated with a method signature.
     *
     * @param sig The signature
     * @return The parameter type of a method signature.
     */
    Class getParameterType(final Rpc.MethodSignature sig);

    /**
     * Execute a method coming in over the wire.
     *
     * @param dc XXXX
     * @param ctx
     * @param message The parameter object.
     * @param noReturn iff no return value is expected.
     * @param multiReturn iff multiple return values are expected.
     */
    void execute(
            final RemoteExecutionContext dc,
            final ChannelHandlerContext ctx,
            final Message message,
            final boolean multiReturn,
            final boolean noReturn);

    /**
     * Cancel an invocation.
     *
     * @param ctx
     * @param rpcIndex
     */
    void startCancel(ChannelHandlerContext ctx, long rpcIndex);

    /**
     * Add an implementation of an interface.
     *
     * @param implementation The instance implementing the RPC interface.
     * @param interfaceClass The class defining the RPC interface.
     */
    void addImplementation(
            final Object implementation,
            final Class interfaceClass) throws RpcServerException;

    /**
     * A new channel was registred as a peer and is prepared to
     * communicate with the execution service.
     * @param ctx  The context for the channel
     */
    public void registerChannel(ChannelHandlerContext ctx);

     /**
     * A new channel was closed as a peer and is  no longer prepared to
     * communicate with the execution service.
     * @param ctx  The context for the channel
     */
    public void channelClosed(ChannelHandlerContext ctx);
}
