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
import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcChannel;
import com.google.protobuf.RpcController;
import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import no.rmz.blobee.rpc.client.RpcClient;
import no.rmz.blobee.rpc.client.RpcClientSideInvocation;
import no.rmz.blobee.rpc.client.RpcClientSideInvocationListener;
import no.rmz.blobee.rpc.peer.RpcMessageListener;
import no.rmz.blobee.rpc.server.RpcServer;
import no.rmz.blobee.rpc.server.RpcServerException;
import no.rmz.blobeetestproto.api.proto.Testservice;
import no.rmz.blobeetestproto.api.proto.Testservice.RpcResult;
import no.rmz.testtools.Net;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.junit.Before;
import org.junit.Test;

public final class SimplePerformanceTest {

    private static final int ROUNDTRIPS = 40000;
    private static final Logger log = Logger.getLogger(
            no.rmz.blobee.rpc.RpcPeerInvocationTest.class.getName());
    private static final String HOST = "localhost";
    private int port;
    private RpcChannel clientChannel;
    private CountDownLatch targetLatch;
    private RpcClient rpcclient;

    public final class TestServiceXX extends Testservice.RpcService {

        public static final String RETURN_VALUE = "Going home";
        private final Testservice.RpcResult result =
                Testservice.RpcResult.newBuilder().setReturnvalue(RETURN_VALUE).build();
        private final CountDownLatch targetLatch;

        public TestServiceXX(final CountDownLatch targetLatch) {
            this.targetLatch = checkNotNull(targetLatch);
        }

        @Override
        public void invoke(
                final RpcController controller,
                final Testservice.RpcParam request,
                final RpcCallback<Testservice.RpcResult> done) {
            targetLatch.countDown();
            final RpcResult returnvalue =
                    Testservice.RpcResult.newBuilder().setReturnvalue(request.getParameter()).build();
            done.run(returnvalue);
        }
    }

    @Before
    @SuppressWarnings("DLS_DEAD_LOCAL_STORE")
    public void setUp() throws IOException, RpcServerException {

        port = Net.getFreePort();

        targetLatch = new CountDownLatch(ROUNDTRIPS);
        final RpcServer rpcServer =
                RpcSetup.newServer(
                new InetSocketAddress(HOST, port),
                new RpcMessageListener() {
                    @Override
                    public void receiveMessage(
                            final Object message,
                            final ChannelHandlerContext ctx) {
                        if (message instanceof Testservice.RpcParam) {
                            Testservice.RpcParam param =
                                    (Testservice.RpcParam) message;
                            final String parameter = param.getParameter();
                            serverReceiverMap.remove(parameter);
                        }
                    }
                })
                .addImplementation(
                new TestServiceXX(targetLatch),
                Testservice.RpcService.Interface.class)
                .start();

        rpcclient =
                RpcSetup
                .newClient(new InetSocketAddress(HOST, port))
                .addInterface(Testservice.RpcService.class)
                .addInvocationListener(new RpcClientSideInvocationListener() {
            @Override
            public void listenToInvocation(
                    final RpcClientSideInvocation invocation) {
                final Message req = invocation.getRequest();
                final Testservice.RpcParam param =
                        (Testservice.RpcParam) req;
                clientSenderMap.remove(param.getParameter());
            }
        })
                .start();

        clientChannel = rpcclient.newClientRpcChannel();
    }
    private final Map<String, Boolean> clientSenderMap =
            new ConcurrentHashMap<>();
    private final Map<String, Boolean> serverReceiverMap =
            new ConcurrentHashMap<>();

    @SuppressWarnings({"WA_AWAIT_NOT_IN_LOOP", "DLS_DEAD_LOCAL_STORE"})
    @Test
    public void testRpcInvocation() throws
            InterruptedException, BrokenBarrierException {

        final CountDownLatch latch = new CountDownLatch(ROUNDTRIPS);
        clientSenderMap.clear();
        serverReceiverMap.clear();

        final Map<String, Boolean> hitMap =
                new ConcurrentHashMap<>();

        final RpcCallback<Testservice.RpcResult> callback =
                new RpcCallback<Testservice.RpcResult>() {
                    @Override
                    public void run(final Testservice.RpcResult response) {
                        latch.countDown();
                        hitMap.remove(response.getReturnvalue());
                    }
                };

        final Testservice.RpcService myService =
                Testservice.RpcService.newStub(clientChannel);


        final long startTime = System.currentTimeMillis();


        for (int i = 0; i < ROUNDTRIPS; i++) {
            final RpcController clientController = rpcclient.newController();
            final String paramstring = Integer.toString(i);
            final Testservice.RpcParam request =
                    Testservice.RpcParam.newBuilder()
                    .setParameter(paramstring)
                    .build();
            hitMap.put(paramstring, Boolean.TRUE);
            clientSenderMap.put(paramstring, Boolean.TRUE);
            serverReceiverMap.put(paramstring, Boolean.TRUE);
            myService.invoke(clientController, request, callback);
        }

        // XXX There are dropouts.  It seems that queries either
        //     get all the way around, or they are dropped on the way
        //     in.  Is there any pattern to this?

        final long expectedMillis = (long) ( 0.025 * ROUNDTRIPS * 12 );
        log.info("This shouldn't take more than " + expectedMillis + " millis");

        latch.await(expectedMillis, TimeUnit.MILLISECONDS);
        final long endTime = System.currentTimeMillis();
        final long duration = endTime - startTime;
        final double millisPerRoundtrip =
                ( (double) duration ) / ( (double) ROUNDTRIPS );

        log.info("Duration of "
                + ROUNDTRIPS
                + " iterations was "
                + duration
                + " milliseconds.  "
                + millisPerRoundtrip
                + " milliseconds per roundtrip.");
        log.info("Latch count "
                + latch.getCount()
                + ", Targetlatch count= "
                + targetLatch.getCount());

        org.junit.Assert.assertEquals("Latch counts should be equal",
                targetLatch.getCount(),
                latch.getCount());
        org.junit.Assert.assertEquals("Count should be zero",
                0,
                latch.getCount());
    }
}
