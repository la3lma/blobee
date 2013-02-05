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

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.protobuf.Service;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import no.rmz.blobee.rpc.client.MultiChannelClientFactory;
import no.rmz.blobee.rpc.peer.RpcMessageListener;
import no.rmz.blobee.rpc.peer.RpcPeerPipelineFactory;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

public final class RpcServerImpl implements RpcServer {

    private final InetSocketAddress socket;
    private final RpcExecutionService executionService;
    private final RpcMessageListener listener;
    private final ServerBootstrap bootstrap;

    public RpcServerImpl(
            final InetSocketAddress socket,
            final RpcMessageListener listener) {
        this(socket,
                new RpcExecutionServiceImpl("Execution service for server listening on " + socket.toString()),
                listener);
    }

     public RpcServerImpl(
            final InetSocketAddress socket,
            final RpcMessageListener listener,
            final ExecutionServiceListener esListener) {
        this(socket,
                new RpcExecutionServiceImpl(
                    "Execution service for server listening on " + socket.toString(),
                    esListener),
                listener);
    }

    public RpcServerImpl(
            final InetSocketAddress socket,
            final RpcExecutionService executionService,
            final RpcMessageListener listener) {
        this.socket = checkNotNull(socket);
        this.executionService = checkNotNull(executionService);
        this.listener = listener; // XXX Nullable
        this.bootstrap = new ServerBootstrap(
                new NioServerSocketChannelFactory(
                    Executors.newCachedThreadPool(),  // XXX Set up using thread factory
                    Executors.newCachedThreadPool()));
        final String name = "RPC Server at " + socket.toString();
        final RpcPeerPipelineFactory serverChannelPipelineFactory =
                new RpcPeerPipelineFactory(name, executionService, new MultiChannelClientFactory(), listener);
        // Set up the pipeline factory.
        bootstrap.setPipelineFactory(serverChannelPipelineFactory);
    }

    public RpcServer start() {
        // Bind and start to accept incoming connections.
        bootstrap.bind(socket);
        return this;
    }

    /**
     * Add a service to the server.
     *
     * @param service
     * @param iface
     * @return
     */
    public RpcServer addImplementation(final Service service, final Class iface) {
        checkNotNull(service);
        checkNotNull(iface);
        try {
            executionService.addImplementation(service, iface);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex); // XXX
        }
        return this;
    }

    public void stop() {
        bootstrap.shutdown();
    }
}
