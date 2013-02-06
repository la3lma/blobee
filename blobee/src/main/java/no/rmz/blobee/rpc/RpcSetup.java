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
package no.rmz.blobee.rpc;

import static com.google.common.base.Preconditions.checkNotNull;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import no.rmz.blobee.rpc.client.ConnectingRpcClientImpl;
import no.rmz.blobee.rpc.client.RpcClient;
import no.rmz.blobee.rpc.client.RpcClientFactory;
import no.rmz.blobee.rpc.client.SingeltonClientFactory;
import no.rmz.blobee.rpc.peer.RpcMessageListener;
import no.rmz.blobee.rpc.peer.RpcPeerPipelineFactory;
import no.rmz.blobee.threads.ErrorLoggingThreadFactory;
import no.rmz.blobee.rpc.server.ExecutionServiceListener;
import no.rmz.blobee.rpc.server.RpcExecutionService;
import no.rmz.blobee.rpc.server.RpcExecutionServiceImpl;
import no.rmz.blobee.rpc.server.RpcServerImpl;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;

/**
 * Setting up an RPC endpoint is about doing these three things:
 *
 * Make a client instance that can be used to send and accept requests. Make
 * a service instance that can be used to serve and return requests. Either -
 * Either connect to a server accepting incoming connection and use that
 * connection to exchange RPC calls. Alternatively, set up a server
 * accepting incoming connections and use those connections to
 * serve incoming requests.
 *
 * So you see, there is a great deal of symmetry here. A client is also a server
 * and a server is also a client.
 *
 */
public final class RpcSetup {

    public final static Logger log = Logger.getLogger(RpcSetup.class.getName());

    /**
     * The default buffer size for the client.  This turns out
     * to be quite significant for the performance. If this queue
     * is too small, the queue will stall and performance will suffer.
     * It seems that 1000 is large enough, but I don't know exactly
     * what the optimal value is, or even better, if it can be calculated
     * dynamically at runtime.
     */
    public final static int DEFAULT_BUFFER_SIZE = 1000;


    /**
     * Utility class, no public constructor for you!
     */
    private RpcSetup() {
    }


    public static RpcClient newClient(
            final InetSocketAddress socketAddress) {

        checkNotNull(socketAddress);

        final RpcExecutionService executor =
                new RpcExecutionServiceImpl("Client execution service");

        final ExecutorService bossExecutor = Executors.newCachedThreadPool(
                new ErrorLoggingThreadFactory("RpcClient bossExecutor", log));

        final ExecutorService workerExcecutor = Executors.newCachedThreadPool(
                new ErrorLoggingThreadFactory("RpcClient workerExcecutor", log));

        // Configure the client.
        // XXX Use a thread factory to catch throws that are not
        //     caught by anyone else.
        final ClientBootstrap clientBootstrap = new ClientBootstrap(
                new NioClientSocketChannelFactory(
                bossExecutor,
                workerExcecutor));

        final RpcClient rpcClient =
                new ConnectingRpcClientImpl(clientBootstrap, socketAddress);

        final String name =
                "RPC client connecting to " + socketAddress.toString();

        final RpcClientFactory rcf = new SingeltonClientFactory(rpcClient);

        final RpcPeerPipelineFactory clientPipelineFactory =
                new RpcPeerPipelineFactory(name, executor, rcf);

        clientBootstrap.setPipelineFactory(clientPipelineFactory);
        return rpcClient;
    }

    public static RpcServerImpl newServer(
            final InetSocketAddress inetSocketAddress,
            final RpcMessageListener rpcMessageListener) {
        checkNotNull(inetSocketAddress);
        return new RpcServerImpl(inetSocketAddress, rpcMessageListener);
    }


    public static RpcServerImpl newServer(
            final InetSocketAddress inetSocketAddress,
            final RpcMessageListener rpcMessageListener,
            final ExecutionServiceListener esListener) {
        checkNotNull(inetSocketAddress);
        return new RpcServerImpl(inetSocketAddress, rpcMessageListener, esListener);
    }

    public static RpcServerImpl newServer(
            final InetSocketAddress inetSocketAddress) {
        return newServer(inetSocketAddress, null);
    }
}
