/**
 * Copyright 2013  Bjørn Remseth (la3lma@gmail.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package no.rmz.blobee.rpc;

import static com.google.common.base.Preconditions.checkNotNull;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;


/**
 * Setting up an RPC endpoint is about doing these three things:
 *
 *    o Make a client instance that can be used to send and
 *      accept requests.
 *    o Make a service instance that can be used to serve and
 *      return requests.
 *    o Either
 *        - Either connect to a server accepting incoming connection and use
 *          that connection to exchange RPC calls.
 *        - Or  set up a server accepting incoming connections and use those
 *          connections to serve incoming requests.
 *
 * So you see, there is a great deal of symmetry here.  A client is also
 * a server and a server is also a client.   This symmetry is not currently
 * reflected in the way we build the server and client instances, but
 * it should.  There should be -very- little difference between the two.
 *
 * Perhaps the main construct should be an "RpcPeer" and then secondarily
 * we should extract either a client or a server depending on our needs?
 */
public final class RpcSetup {

    /**
     * Utility class, no public constructor for you!
     */
    private RpcSetup() {
    }

    public final static class Node {

        final RpcExecutionService executionService;
        final RpcClientFactory rcf;

        private Node(final RpcExecutionService executor,
                     final RpcClientFactory rcf) {
            this.executionService = checkNotNull(executor);
            this.rcf = checkNotNull(rcf);
        }

        final RpcClientImpl getClient(final Channel channel) {
            checkNotNull(channel);
            return rcf.getClientFor(channel);
        }
    }

    final int bufferSize = 1; // XXX


    public final static class SingeltonClientFactory implements RpcClientFactory {

        private final Object monitor = new Object();

        private final RpcClient rpcClient;
        private Channel channel;

        public SingeltonClientFactory(final RpcClient rpcClient) {
            this.rpcClient = checkNotNull(rpcClient);
        }


        public RpcClientImpl getClientFor(final Channel channel) {
            synchronized (monitor) {
                if (this.channel == null) {
                    this.channel = channel;
                } if (channel != this.channel) {
                    throw new IllegalStateException(
                            "Attempt to get client for more than one channel "
                            + channel);
                } else {
                    return rpcClient;
                }
            }
        }
    }

    // XXX Next step: Make sure this implementation works!
    public static RpcClient newConnectingNode(RpcExecutionService executor, final InetSocketAddress socketAddress) {
        checkNotNull(socketAddress);

/*
        final RpcExecutionService executor =
                new RpcExecutionServiceImpl();
*/
        // Configure the client.
        final ClientBootstrap clientBootstrap = new ClientBootstrap(
                new NioClientSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool()));

        final RpcClient rpcClient =
                new ConnectingRpcClientImpl(clientBootstrap, socketAddress);


        final String name =
                "A client";

        final RpcClientFactory rcf = new SingeltonClientFactory(rpcClient);

        final RpcPeerPipelineFactory clientPipelineFactory =
                new RpcPeerPipelineFactory(name, executor, rpcClient);

        clientBootstrap.setPipelineFactory(clientPipelineFactory);
        return rpcClient;
//        return new Node(executor, rcf);
    }


    // XXX If this works, that's just a lucky break.
    public void newAcceptingNode(final int port) {

        final ServerBootstrap bootstrap = new ServerBootstrap(
                new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool()));

        final String name = "server at port " + port;

        final RpcExecutionService executionService =
                new RpcExecutionServiceImpl();

        // XXX This is completely wrong, this should be an
        //     RpcClientFactory
        final RpcClientImpl rpcClient = new RpcClientImpl(bufferSize);

        final RpcPeerPipelineFactory serverChannelPipelineFactory =
                new RpcPeerPipelineFactory(
                    "server accepting incoming connections at port ",
                    executionService, rpcClient);

        // XXX Something missing to set channel for client.

        // Set up the pipeline factory.
        bootstrap.setPipelineFactory(serverChannelPipelineFactory);

        // Bind and start to accept incoming connections.
        bootstrap.bind(new InetSocketAddress(port));
    }



    @Deprecated
    public static void setUpServer(
            final int port,
            final RpcExecutionService executionService,
            final RpcClient rpcClient,
            final RpcMessageListener listener) {

        final ServerBootstrap bootstrap = new ServerBootstrap(
                new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool()));

        final String name = "server at port " + port;

        final RpcPeerPipelineFactory serverChannelPipelineFactory =
                new RpcPeerPipelineFactory(
                "server accepting incoming connections at port ", executionService, rpcClient, listener);

        // Set up the pipeline factory.
        bootstrap.setPipelineFactory(serverChannelPipelineFactory);

        // Bind and start to accept incoming connections.
        bootstrap.bind(new InetSocketAddress(port));
    }



    public static RpcClientImpl setUpClient(
            final RpcExecutionService executor) {
        return setUpClient(executor, null);
    }

    @Deprecated
    public static RpcClient setUpClient(
            final RpcExecutionService executor,
            final RpcMessageListener listener) {


        final int bufferSize = 1;
        final RpcClientImpl rpcClient = new RpcClientImpl(bufferSize);

         // Configure the client.
        final ClientBootstrap clientBootstrap = new ClientBootstrap(
                new NioClientSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool()));
        final String name =
               "A client";
        final RpcPeerPipelineFactory clientPipelineFactory =
                new RpcPeerPipelineFactory(name, executor,  rpcClient, listener);

        clientBootstrap.setPipelineFactory(
                clientPipelineFactory);

        // rpcClient.setClientPipelineFactory(clientPipelineFactory);
       //  rpcClient.setBootstrap(clientBootstrap);

        return rpcClient;
    }
}
