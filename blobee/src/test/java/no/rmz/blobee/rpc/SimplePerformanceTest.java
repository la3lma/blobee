package no.rmz.blobee.rpc;

import no.rmz.blobee.rpc.peer.RpcMessageListener;
import no.rmz.blobee.rpc.server.ExecutionServiceException;
import no.rmz.blobee.rpc.server.ExecutionServiceListener;
import no.rmz.blobee.rpc.server.RpcServer;
import no.rmz.blobee.rpc.client.RpcClient;
import no.rmz.blobee.rpc.client.RpcClientSideInvocationListener;
import no.rmz.blobee.rpc.client.RpcClientSideInvocation;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcChannel;
import com.google.protobuf.RpcController;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import no.rmz.blobee.controllers.RpcServiceController;
import no.rmz.blobeeprototest.api.proto.Testservice;
import no.rmz.blobeeprototest.api.proto.Testservice.RpcResult;
import no.rmz.testtools.Net;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.junit.Before;
import org.junit.Test;


public class SimplePerformanceTest {

    final int ROUNDTRIPS = 400000;
    final int DELTA = 0;

    private static final Logger log = Logger.getLogger(
            no.rmz.blobee.rpc.RpcPeerInvocationTest.class.getName());
    private final static String HOST = "localhost";

    private int port;

    private RpcChannel clientChannel;
    private Testservice.RpcParam request = Testservice.RpcParam.newBuilder().build();

    private CountDownLatch targetLatch;

    RpcClient rpcclient;

    public final class TestServiceXX extends Testservice.RpcService {

        public final static String RETURN_VALUE = "Going home";
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

    private ExecutorService executorService;

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
        executorService = null;

        targetLatch = new CountDownLatch(ROUNDTRIPS);
        final RpcServer rpcServer =
                RpcSetup.nyServer(
                    new InetSocketAddress(HOST, port),
                    new RpcMessageListener() {

                    public void receiveMessage(Object message, ChannelHandlerContext ctx) {
                        if (message instanceof Testservice.RpcParam) {
                            Testservice.RpcParam param =(Testservice.RpcParam) message;
                                final String parameter = param.getParameter();
                            serverReceiverMap.remove(parameter);
                        }
                    }

                    }, new ExecutionServiceListener() {

                    public void listen(
                            ExecutorService ex,
                            Object method,
                            Object implementation,
                            Object controller,
                            Object parameter,
                            Object callback) {
                        executorService = ex;
                       final Testservice.RpcParam trp = (Testservice.RpcParam) parameter;
                       final String paramString = trp.getParameter();
                       serverExecutorMap.remove(paramString);
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

                    public void listenToInvocation(final RpcClientSideInvocation invocation) {
                        final Message req = invocation.getRequest();
                        final Testservice.RpcParam  param = (Testservice.RpcParam) req;
                        clientSenderMap.remove(param.getParameter());
                    }
                })
                .start();

        clientChannel    = rpcclient.newClientRpcChannel();
    }

    final Map<String, Boolean> serverExecutorMap = new ConcurrentHashMap<String, Boolean>();
    final Map<String, Boolean> clientSenderMap = new ConcurrentHashMap<String, Boolean>();
    final Map<String, Boolean> serverReceiverMap = new ConcurrentHashMap<String, Boolean>();


    @edu.umd.cs.findbugs.annotations.SuppressWarnings("WA_AWAIT_NOT_IN_LOOP")
    @Test
    public void testRpcInvocation() throws InterruptedException, BrokenBarrierException {

        final CountDownLatch latch = new CountDownLatch(ROUNDTRIPS);
        clientSenderMap.clear();
        serverExecutorMap.clear();
        serverReceiverMap.clear();

        final Map<String, Boolean> hitMap = new ConcurrentHashMap<String, Boolean>();

        final RpcCallback<Testservice.RpcResult> callback =
                new RpcCallback<Testservice.RpcResult>() {
                    public void run(final Testservice.RpcResult response) {
                        latch.countDown();
                        hitMap.remove(response.getReturnvalue());
                    }
                };

        final Testservice.RpcService myService = Testservice.RpcService.newStub(clientChannel);


        final long startTime = System.currentTimeMillis();


        for (int i = 0; i < ROUNDTRIPS + DELTA ; i++) {
            final RpcController clientController = rpcclient.newController();
            final String paramstring = Integer.toString(i);
            final Testservice.RpcParam request =
                     Testservice.RpcParam.newBuilder()
                    .setParameter(paramstring)
                    .build();
            hitMap.put(paramstring, Boolean.TRUE);
            clientSenderMap.put(paramstring, Boolean.TRUE);
            serverReceiverMap.put(paramstring, Boolean.TRUE);
            serverExecutorMap.put(paramstring, Boolean.TRUE);
            myService.invoke(clientController, request, callback);
        }

        // XXX There are dropouts.  It seems that queries either
        //     get all the way around, or they are dropped on the way
        //     in.  Is there any pattern to this?

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
                + latch.getCount()
                + ", Targetlatch count= "
                + targetLatch.getCount());

        log.info("Requests that didn't get through all the way: " + hitMap.keySet().toString());
        log.info("Requests that were not transmitted to the wire: " + clientSenderMap.keySet().toString());
        log.info("Requests that did not get through to the server: " + serverReceiverMap.keySet().toString());
        log.info("Requests that were not invoked by the server: " + serverExecutorMap.keySet().toString());

        log.info("count(serverReceiverMap): " + serverReceiverMap.keySet().size());
        log.info("count(clientSenderMap): " + clientSenderMap.keySet().size());
        log.info("count(serverExecutorMap): " + serverExecutorMap.keySet().size());

        log.info("Jobs still pending in the executor service " + executorService.shutdownNow());

        org.junit.Assert.assertEquals("Latch counts should be equal",
                targetLatch.getCount(),
                latch.getCount());
        org.junit.Assert.assertEquals("Count should be zero",
                0,
                latch.getCount());
    }
}