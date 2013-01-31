package no.rmz.blobee.rpc;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcChannel;
import com.google.protobuf.RpcController;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import no.rmz.blobeeprototest.api.proto.Testservice;
import no.rmz.blobeeprototest.api.proto.Testservice.RpcResult;
import no.rmz.testtools.Net;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.junit.Before;
import org.junit.Test;


public class ReallySimplePerformanceTest {

    final int ROUNDTRIPS = 400000;

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
                new TestServiceXX(),
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

        final CountDownLatch latch = new CountDownLatch(ROUNDTRIPS);

        final RpcCallback<Testservice.RpcResult> callback =
                new RpcCallback<Testservice.RpcResult>() {
                    public void run(final Testservice.RpcResult response) {
                        latch.countDown();
                    }
                };

        final Testservice.RpcService myService = Testservice.RpcService.newStub(clientChannel);

        final long startTime = System.currentTimeMillis();

        for (int i = 0; i < ROUNDTRIPS ; i++) {
            final RpcController clientController = rpcclient.newController();
            myService.invoke(clientController, request, callback);
        }

        final double expectedTime = 0.025 * ROUNDTRIPS * 12;

        final long expectedMillis = (long) expectedTime;
        log.info("This shouldn't take more than " + expectedMillis + " millis");

        latch.await((long) expectedTime, TimeUnit.MILLISECONDS);
        final long endTime = System.currentTimeMillis();
        final long duration = endTime - startTime;
        final double millisPerRoundtrip = (double)duration / (double)ROUNDTRIPS;

        log.info("Duration of "
                + ROUNDTRIPS
                + " iterations was "
                + duration
                + " milliseconds.  "
                + millisPerRoundtrip
                + " milliseconds per roundtrip.");
        log.info("Latch count "
                + latch.getCount());
        org.junit.Assert.assertEquals("Count should be zero",
                0,
                latch.getCount());
    }
}
