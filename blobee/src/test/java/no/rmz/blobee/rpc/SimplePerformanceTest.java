package no.rmz.blobee.rpc;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcChannel;
import com.google.protobuf.RpcController;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;
import no.rmz.blobee.serviceimpls.SampleServerImpl;
import no.rmz.blobeeprototest.api.proto.Testservice;
import no.rmz.testtools.Net;
import org.junit.Before;
import org.junit.Test;


public class SimplePerformanceTest {


    private static final Logger log = Logger.getLogger(
            no.rmz.blobee.rpc.RpcPeerInvocationTest.class.getName());
    private final static String HOST = "localhost";

    private int port;

    private RpcChannel clientChannel;
    private Testservice.RpcParam request = Testservice.RpcParam.newBuilder().build();


    RpcClient rpcclient;


    @Before
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
                new SampleServerImpl(),
                Testservice.RpcService.Interface.class)
                .start();

        rpcclient =
                RpcSetup
                .newClient(new InetSocketAddress(HOST, port))
                .addInterface(Testservice.RpcService.class)
                .start();

        clientChannel    = rpcclient.newClientRpcChannel();
    }


    @edu.umd.cs.findbugs.annotations.SuppressWarnings("WA_AWAIT_NOT_IN_LOOP")
    @Test
    public void testRpcInvocation() throws InterruptedException, BrokenBarrierException {

        final int roundtrips = 5000;

        final CountDownLatch latch = new CountDownLatch(roundtrips);

        final RpcCallback<Testservice.RpcResult> callback =
                new RpcCallback<Testservice.RpcResult>() {
                    public void run(final Testservice.RpcResult response) {
                        latch.countDown();
                    }
                };

        final Testservice.RpcService myService = Testservice.RpcService.newStub(clientChannel);

        final long startTime = System.currentTimeMillis();
        for (int i = 0; i < roundtrips; i++) {
             final RpcController clientController = rpcclient.newController();
            myService.invoke(clientController, request, callback);
        }

        latch.await();
        final long endTime = System.currentTimeMillis();
        final long duration = endTime - startTime;
        final double millisPerRoundtrip = (double)duration / (double)roundtrips;
        log.info("Duration of "
                + roundtrips
                + " iterations was "
                + duration
                + " milliseconds.  "
                + millisPerRoundtrip
                + " milliseconds per roundtrip.");
    }
}
