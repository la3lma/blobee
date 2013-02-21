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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import no.rmz.blobee.rpc.client.MultiChannelClientFactory;
import no.rmz.blobee.rpc.peer.RpcMessageListener;
import no.rmz.blobee.rpc.peer.RpcPeerPipelineFactory;
import no.rmz.blobee.threads.ErrorLoggingThreadFactory;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

/**
 * An implementation of an RpcServer that will listen for incoming
 * TCP connections on a given InetSocketAddress, and will then handle
 * incoming RPC connections on that TCP connection.
 */
public final class RpcServerImpl implements RpcServer {

    private static final  Logger log =
            Logger.getLogger(RpcServerImpl.class.getName());
    private final InetSocketAddress socket;
    private final RpcExecutionService executionService;
    private final RpcMessageListener listener;
    private final ServerBootstrap bootstrap;

    /**
     * A thread pool.
     */
    private final ExecutorService bossExecutor;

    /**
     * Another thread pool.
     */
    private final ExecutorService workerExcecutor;

    /**
     * Construct a new server implementation that will listen for
     * incoming client connections on the specified socket.
     *
     * The optional listener argument is used while debugging to listen
     * in on incoming messages.
     *
     * @param socket The socket to listen for.
     * @param listener  An optional listener.  IF non-null, this listener
     *        will listen in on all the messages flowing into the server.
     */
    public RpcServerImpl(
            final InetSocketAddress socket,
            final RpcMessageListener listener) {
        this(socket,
                new RpcExecutionServiceImpl(
                "Execution service for server listening on "
                + socket.toString()),
                listener);
    }


    public RpcServerImpl(
            final InetSocketAddress socket,
            final RpcExecutionService executionService,
            final RpcMessageListener listener) {
        this.socket = checkNotNull(socket);
        this.executionService = checkNotNull(executionService);
        this.listener = listener; // XXX Nullable

        bossExecutor = Executors.newCachedThreadPool(
                new ErrorLoggingThreadFactory(
                "RpcServerImpl bossExecutor", log));

        workerExcecutor = Executors.newCachedThreadPool(
                new ErrorLoggingThreadFactory(
                "RpcServerImpl workerExcecutor", log));

        this.bootstrap = new ServerBootstrap(
                new NioServerSocketChannelFactory(
                bossExecutor,
                workerExcecutor));
        final String name = "RPC Server at " + socket.toString();
        final RpcPeerPipelineFactory serverChannelPipelineFactory =
                new RpcPeerPipelineFactory(
                      name,
                      executionService,
                      new MultiChannelClientFactory(),
                      listener);
        // Set up the pipeline factory.
        bootstrap.setPipelineFactory(serverChannelPipelineFactory);
    }

    /**
     * Bind to bootstrap socket and start accepting incoming connections and
     * requests.
     *
     * @return this
     */
    @Override
    public RpcServer start() {
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
    public RpcServer addImplementation(
            final Service service,
            final Class iface) throws RpcServerException {
        checkNotNull(service);
        checkNotNull(iface);
        executionService.addImplementation(service, iface);
        return this;
    }

    @Override
    public void stop() {
        bootstrap.shutdown();
        bossExecutor.shutdownNow();
        workerExcecutor.shutdownNow();
    }
}
