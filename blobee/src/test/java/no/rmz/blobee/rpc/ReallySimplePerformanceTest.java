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

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcChannel;
import com.google.protobuf.RpcController;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import no.rmz.blobee.rpc.client.RpcClient;
import no.rmz.blobee.rpc.server.ExecutionServiceException;
import no.rmz.blobee.rpc.server.RpcServer;
import no.rmz.blobeeprototest.api.proto.Testservice;
import no.rmz.blobeeprototest.api.proto.Testservice.RpcResult;
import no.rmz.testtools.Net;

public class ReallySimplePerformanceTest {

    private final static int ROUNDTRIPS = 4000000;
    private static final Logger log = Logger.getLogger(
            no.rmz.blobee.rpc.RpcPeerInvocationTest.class.getName());
    private final static String HOST = "localhost";
    private int port;
    private RpcChannel clientChannel;
    private Testservice.RpcParam request = Testservice.RpcParam.newBuilder().build();
    RpcClient rpcclient;

    public final class TestServiceXX extends Testservice.RpcService {

        public final static String RETURN_VALUE = "Going home";
        private final Testservice.RpcResult result =
                Testservice.RpcResult.newBuilder().setReturnvalue(RETURN_VALUE).build();
        final RpcResult returnvalue =
                Testservice.RpcResult.newBuilder().setReturnvalue(request.getParameter()).build();

        @Override
        public void invoke(
                final RpcController controller,
                final Testservice.RpcParam request,
                final RpcCallback<Testservice.RpcResult> done) {
            done.run(returnvalue);
        }
    }

    public void setUp() throws
            NoSuchMethodException,
            IllegalAccessException,
            IllegalArgumentException,
            InvocationTargetException,
            IOException,
            SecurityException,
            IllegalStateException,
            ExecutionServiceException {

        port = Net.getFreePort();

        final RpcServer rpcServer =
                RpcSetup.nyServer(
                new InetSocketAddress(HOST, port))
                .addImplementation(
                new TestServiceXX(),
                Testservice.RpcService.Interface.class)
                .start();

        rpcclient =
                RpcSetup
                .newClient(new InetSocketAddress(HOST, port))
                .addInterface(Testservice.RpcService.class)
                .start();

        clientChannel = rpcclient.newClientRpcChannel();
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings("WA_AWAIT_NOT_IN_LOOP")
    public void testRpcInvocation() throws InterruptedException, BrokenBarrierException {

        final CountDownLatch latch = new CountDownLatch(ROUNDTRIPS);

        final RpcCallback<Testservice.RpcResult> callback =
                new RpcCallback<Testservice.RpcResult>() {
            public void run(final Testservice.RpcResult response) {
                latch.countDown();
            }
        };

        final Testservice.RpcService myService = Testservice.RpcService.newStub(clientChannel);

        final long startTime = System.currentTimeMillis();

        for (int i = 0; i < ROUNDTRIPS; i++) {
            final RpcController clientController = rpcclient.newController();
            myService.invoke(clientController, request, callback);
        }

        final int marginFactor = 2;

        final double expectedTime = 0.025 * ROUNDTRIPS * marginFactor;

        final long expectedMillis = (long) expectedTime;
        log.info("This shouldn't take more than "
                + expectedMillis
                + " millis (margin factor = "
                + marginFactor
                + ")");


        latch.await();
//        latch.await((long) expectedTime, TimeUnit.MILLISECONDS);
        final long endTime = System.currentTimeMillis();
        final long duration = endTime - startTime;
        final double millisPerRoundtrip = (double) duration / (double) ROUNDTRIPS;

        log.info("Duration of "
                + ROUNDTRIPS
                + " iterations was "
                + duration
                + " milliseconds.  "
                + millisPerRoundtrip
                + " milliseconds per roundtrip.");
        log.info("Latch count "
                + latch.getCount());


    }

    public final static void main(final String argv[])  throws Exception {
        final ReallySimplePerformanceTest tst = new ReallySimplePerformanceTest();
        tst.setUp();
        tst.testRpcInvocation();
        System.exit(0);
    }
}
