package no.rmz.blobee.rpc;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcChannel;
import com.google.protobuf.RpcController;
import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import no.rmz.blobee.serviceimpls.SampleServerImpl;
import no.rmz.blobee.serviceimpls.SampleServerImpl1;
import no.rmz.blobee.rpc.RpcClient;
import no.rmz.blobee.rpc.RpcExecutionService;
import no.rmz.blobee.rpc.RpcExecutionServiceImpl;
import no.rmz.blobee.rpc.RpcMessageListener;
import no.rmz.blobee.rpc.RpcSetup;
import no.rmz.blobeetestproto.api.proto.Tullball.RpcPar;
import no.rmz.blobeetestproto.api.proto.Tullball.RpcRes;
import no.rmz.blobeetestproto.api.proto.Tullball.RpcServ;
import no.rmz.testtools.Net;
import no.rmz.testtools.Receiver;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public final class RpcPeerInvocation1Test {

    private static final Logger log = Logger.getLogger(
            no.rmz.blobee.rpc.RpcPeerInvocation1Test.class.getName());
    private final static String HOST = "localhost";

    private int port;

    private RpcChannel clientChannel;
    private RpcPar request = RpcPar.newBuilder().build();
    private RpcController clientController;

    RpcMessageListener rpcMessageListener = new RpcMessageListener() {
        public void receiveMessage(
                final Object message,
                final ChannelHandlerContext ctx) {
            log.log(Level.INFO, "message = {0}", message);
        }
    };
    private Lock lock;
    private Condition resultReceived;
    private RpcController servingController;

    private void signalResultReceived() {
        try {
            lock.lock();
            resultReceived.signal();
        }
        finally {
            lock.unlock();
        }
    }

    @Before
    public void setUp() throws
            NoSuchMethodException,
            IllegalAccessException,
            IllegalArgumentException,
            InvocationTargetException,
            IOException {

        lock = new ReentrantLock();
        resultReceived = lock.newCondition();
        port = Net.getFreePort();

        final RpcExecutionService executionService;
        executionService = new RpcExecutionServiceImpl(
                new SampleServerImpl1(),  // XXX No actual typechecking here!!
                RpcServ.Interface.class);

        final RpcClient client = RpcSetup.setUpClient(HOST, port, executionService);

        final RpcClient serversClient = client; // XXX This is an abomination
        RpcSetup.setUpServer(port, executionService, serversClient, rpcMessageListener);

        client.start();

        clientChannel    = client.newClientRpcChannel();
        clientController = client.newController();
    }

    @Mock
    Receiver<String> callbackResponse;

    @Test
    @SuppressWarnings("WA_AWAIT_NOT_IN_LOOP")
    public void testRpcInvocation() throws InterruptedException {

        final RpcCallback<RpcRes> callback =
                new RpcCallback<RpcRes>() {
                    public void run(final RpcRes response) {
                        callbackResponse.receive(response.getReturnvalue());
                        signalResultReceived();
                    }
                };

        final RpcServ myService;
        myService = RpcServ.newStub(clientChannel);
        myService.invoke(clientController, request, callback);

        try {
            lock.lock();
            log.info("Awaiting result received.");
            resultReceived.await(300, TimeUnit.SECONDS); // XXX Should be tuned a bit:-)
        } finally {
            lock.unlock();
            log.info("unlocked, test passed");
        }

        verify(callbackResponse).receive(SampleServerImpl.RETURN_VALUE);
    }
}
