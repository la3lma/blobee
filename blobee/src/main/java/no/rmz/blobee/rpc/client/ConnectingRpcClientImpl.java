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

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.protobuf.Message;
import com.google.protobuf.RpcChannel;
import com.google.protobuf.RpcController;
import java.net.InetSocketAddress;
import no.rmz.blobee.rpc.RpcSetup;
import no.rmz.blobee.rpc.methods.MethodSignatureResolver;
import no.rmz.blobee.rpc.peer.RemoteExecutionContext;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelFuture;

/**
 * An RpcClient  implementation that will open a connection to a
 * particular InetSocketAddress.
 */
public final class ConnectingRpcClientImpl implements RpcClient {
    private final RpcClientImpl rpcClient;
    private final ClientBootstrap clientBootstrap;
    private final InetSocketAddress socketAddress;

    /**
     * Create a client that will connect to a remote service.
     * @param clientBootstrap A ClientBootstrap used by the client to connect.
     * @param socketAddress  The socket to connect to.
     */
    public ConnectingRpcClientImpl(
            final ClientBootstrap clientBootstrap,
            final InetSocketAddress socketAddress) {
        this.rpcClient = new RpcClientImpl(RpcSetup.DEFAULT_BUFFER_SIZE);
        this.socketAddress = checkNotNull(socketAddress);
        // XXX BOGUS
        this.clientBootstrap = checkNotNull(clientBootstrap);
    }

    // XXX This stuff needs to be rewritten so it can work
    //     without having to start its own ChannelFuture.  It should
    //     be possible to graft the thing onto a server instance.
    @Override
    public RpcClient start() {
        checkNotNull(socketAddress);

        // Start the connection attempt.
        final ChannelFuture future = clientBootstrap.connect(socketAddress);
        rpcClient.start(future.getChannel(), new ChannelShutdownCleaner() {
            @Override
            public void shutdownHook() {
                // Shut down thread pools to exit.
                clientBootstrap.releaseExternalResources();
            }
        });
        return this;
    }

    @Override
    public void cancelInvocation(final long rpcIndex) {
        rpcClient.cancelInvocation(rpcIndex);
    }

    @Override
    public void failInvocation(final long rpcIndex, final String errorMessage) {
        rpcClient.failInvocation(rpcIndex, errorMessage);
    }

    @Override
    public RpcChannel newClientRpcChannel() {
        return rpcClient.newClientRpcChannel();
    }

    @Override
    public BlobeeRpcController newController() {
        return rpcClient.newController();
    }

    @Override
    public void returnCall(
            final RemoteExecutionContext dc,
            final Message message) {
        checkNotNull(dc);
        checkNotNull(message);
        rpcClient.returnCall(dc, message);
    }


    @Override
    public MethodSignatureResolver getResolver() {
       return rpcClient.getResolver();
    }

    @Override
    public RpcClient addProtobuferRpcInterface(final Object instance) {
         rpcClient.addProtobuferRpcInterface(instance);
         return this;
    }

    @Override
    public RpcClient addInterface(final Class serviceInterface) {
         rpcClient.addInterface(serviceInterface);
         return this;
    }

    @Override
    public RpcClient addInvocationListener(
            final RpcClientSideInvocationListener listener) {
        rpcClient.addInvocationListener(listener);
        return this;
    }

    @Override
    public void stop() {
        rpcClient.stop();
    }

    @Override
    public void terminateMultiSequence(long rpcIdx) {
      rpcClient.terminateMultiSequence(rpcIdx);
    }
}
