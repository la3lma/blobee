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

import com.google.protobuf.MessageLite;
import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import no.rmz.blobee.handler.codec.protobuf.DynamicProtobufDecoder;
import no.rmz.blobee.rpc.WireFactory;
import no.rmz.blobeeproto.api.proto.Rpc;
import no.rmz.blobeeprototest.api.proto.Testservice;
import no.rmz.testtools.Net;
import no.rmz.testtools.Receiver;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import static org.jboss.netty.channel.Channels.pipeline;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.protobuf.ProtobufEncoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * An essential test: Testing that we can send two completely different types of
 * things over the same netty channel.
 */
@RunWith(MockitoJUnitRunner.class)
public final class AlternatingTypeChannelTest {

    private static final Logger log = Logger.getLogger(
            no.rmz.blobee.rpc.AlternatingTypeChannelTest.class.getName());
    private final static String PARAMETER_STRING = "Hello server";
    private final static String HOST = "localhost";
    private final static int FIRST_MESSAGE_SIZE = 256;
    private int port;
    // This is the receptacle for the message that goes
    // over the wire.
    @Mock
    Receiver<Testservice.RpcParam> serverParamReceiver;
    @Mock
    private Receiver<Rpc.RpcControl> serverControlReceiver;
    private Testservice.RpcParam sampleRpcMessage;
    private Rpc.RpcControl sampleControlMessage;
    private AdaptiveDecoder serverChannelPipelineFactory;
    private AdaptiveDecoder clientPipelineFactory;

    @Before
    public void setUp() throws IOException {

        port = Net.getFreePort();

        sampleRpcMessage =
                Testservice.RpcParam.newBuilder().setParameter(PARAMETER_STRING).build();
        sampleControlMessage =
                Rpc.RpcControl.newBuilder()
                .setMessageType(Rpc.MessageType.RPC_RETURNVALUE)
                .setStat(Rpc.StatusCode.OK)
                .build();

        serverChannelPipelineFactory = new AdaptiveDecoder("server",
                new SimpleChannelUpstreamHandlerFactory() {
                    public SimpleChannelUpstreamHandler newHandler() {
                        return new RpcServerHandler();
                    }
                });


        clientPipelineFactory = new AdaptiveDecoder("client",
                new SimpleChannelUpstreamHandlerFactory() {
                    public SimpleChannelUpstreamHandler newHandler() {
                        return new RpcClientHandler();
                    }
                });
    }

    public final class RpcServerHandler extends SimpleChannelUpstreamHandler {

        volatile int counter = 0;

        @Override
        public void messageReceived(
                final ChannelHandlerContext ctx, final MessageEvent e) {
            final Object message = e.getMessage();

            log.info("Received message " + message);

            if (message instanceof Testservice.RpcParam) {
                final Testservice.RpcParam msg = (Testservice.RpcParam) e.getMessage();
                serverParamReceiver.receive(msg);
                counter += 1;

                serverChannelPipelineFactory.isControl = true;
                serverChannelPipelineFactory.putNextPrototype(Rpc.RpcControl.getDefaultInstance());
            } else if (message instanceof Rpc.RpcControl) {
                final Rpc.RpcControl msg = (Rpc.RpcControl) e.getMessage();
                serverControlReceiver.receive(msg);
                counter += 1;

                serverChannelPipelineFactory.isControl = false;
                serverChannelPipelineFactory.putNextPrototype(Testservice.RpcParam.getDefaultInstance());
            } else {
                fail("Unknown type of incoming message to server: " + message);
            }

            // For this test, this is actually a proper termination
            // criterion.
            if (counter > 1) {
                e.getChannel().close();
            }
        }

        @Override
        public void exceptionCaught(
                ChannelHandlerContext ctx, ExceptionEvent e) {
            // Close the connection when an exception is raised.
            log.log(
                    Level.WARNING,
                    "Unexpected exception from downstream.",
                    e.getCause());
            e.getChannel().close();
        }
    }

    public final class RpcClientHandler extends SimpleChannelUpstreamHandler {

        @Override
        public void channelConnected(
                final ChannelHandlerContext ctx, final ChannelStateEvent e) {
            WireFactory.getWireForChannel(e.getChannel())
                    .write(
                        sampleControlMessage,
                        sampleRpcMessage);
        }

        @Override
        public void messageReceived(
                final ChannelHandlerContext ctx, final MessageEvent e) {
            final Object message = e.getMessage();
            log.info("The client received message object : " + message);
            final Testservice.RpcResult result = (Testservice.RpcResult) e.getMessage();
            log.info("The client received result: " + result);
            clientPipelineFactory.putNextPrototype(Rpc.RpcControl.getDefaultInstance());
        }

        @Override
        public void exceptionCaught(
                final ChannelHandlerContext ctx, final ExceptionEvent e) {
            // Close the connection when an exception is raised.
            log.log(
                    Level.WARNING,
                    "Unexpected exception from downstream.",
                    e.getCause());
            e.getChannel().close();
            fail();
        }
    }

    public interface SimpleChannelUpstreamHandlerFactory {

        SimpleChannelUpstreamHandler newHandler();
    }

    public final class AdaptiveDecoder implements ChannelPipelineFactory {

        private final String name;
        final SimpleChannelUpstreamHandlerFactory upstreamHandlerFactory;
        private boolean isControl = true;
        private final Lock lock = new ReentrantLock();
        private Condition inputIsParsable;
        private Condition inputIsParsed;
        private boolean firstTime = true;

        public AdaptiveDecoder(final String name,
                final SimpleChannelUpstreamHandlerFactory upstreamHandlerFactory) {
            this.upstreamHandlerFactory = upstreamHandlerFactory;
            this.name = name;
        }
        private final DynamicProtobufDecoder protbufDecoder =
                new DynamicProtobufDecoder();

        public void putNextPrototype(final MessageLite prototype) {
            protbufDecoder.putNextPrototype(prototype.getDefaultInstanceForType());
        }

        @SuppressWarnings("WA_AWAIT_NOT_IN_LOOP")
        public ChannelPipeline getPipeline() throws Exception {
            lock.lock();
            try {

                final ChannelPipeline p = pipeline();
                p.addLast("frameDecoder", new ProtobufVarint32FrameDecoder());
                p.addLast("protobufDecoder", protbufDecoder);
                p.addLast("frameEncoder", new ProtobufVarint32LengthFieldPrepender());
                p.addLast("protobufEncoder", new ProtobufEncoder());
                p.addLast("handler", upstreamHandlerFactory.newHandler());
                return p;
            }
            finally {
                lock.unlock();
            }
        }
    }

    public void setUpServer() {

        final ServerBootstrap bootstrap = new ServerBootstrap(
                new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool()));

        // Set up the pipeline factory.
        bootstrap.setPipelineFactory(serverChannelPipelineFactory);
        serverChannelPipelineFactory.putNextPrototype(Rpc.RpcControl.getDefaultInstance());
        // Bind and start to accept incoming connections.
        final InetSocketAddress inetSocketAddress = new InetSocketAddress(port);
        bootstrap.bind(inetSocketAddress);
    }

    private void setUpClient() {

        // Configure the client.
        final ClientBootstrap clientBootstrap = new ClientBootstrap(
                new NioClientSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool()));

        clientBootstrap.setPipelineFactory(
                clientPipelineFactory);
        clientPipelineFactory.putNextPrototype(Rpc.RpcControl.getDefaultInstance());

        // Start the connection attempt.
        final ChannelFuture future =
                clientBootstrap.connect(new InetSocketAddress(HOST, port));

        // Wait until the connection is closed or the connection attempt fails.
        future.getChannel().getCloseFuture().awaitUninterruptibly();

        // Shut down thread pools to exit.
        clientBootstrap.releaseExternalResources();
    }

    @Test
    public void testTransmission() {
        setUpServer();
        setUpClient();

        verify(serverControlReceiver).receive(sampleControlMessage);
        verify(serverParamReceiver).receive(sampleRpcMessage);
    }
}