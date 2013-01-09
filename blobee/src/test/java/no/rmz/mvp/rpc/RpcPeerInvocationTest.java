package no.rmz.mvp.rpc;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcChannel;
import com.google.protobuf.RpcController;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import no.rmz.blobee.SampleServerImpl;
import no.rmz.blobee.rpc.RpcClient;
import no.rmz.blobee.rpc.RpcExecutionService;
import no.rmz.blobee.rpc.RpcMessageListener;
import no.rmz.blobee.rpc.RpcSetup;
import no.rmz.blobee.rpc.ServingRpcChannel;
import no.rmz.blobeeproto.api.proto.Rpc;
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
public final class RpcPeerInvocationTest {

    private static final Logger log = Logger.getLogger(
            no.rmz.mvp.rpc.RpcPeerInvocationTest.class.getName());
    private final static String HOST = "localhost";
    private final static Rpc.RpcControl HEARTBEAT_MESSAGE =
            Rpc.RpcControl.newBuilder().setMessageType(Rpc.MessageType.HEARTBEAT).build();
    private final static Rpc.RpcControl SHUTDOWN_MESSAGE =
            Rpc.RpcControl.newBuilder().setMessageType(Rpc.MessageType.SHUTDOWN).build();
    private int port;
    private ServingRpcChannel servingChannel;
    private RpcChannel clientChannel;
    private Rpc.RpcParam request = Rpc.RpcParam.newBuilder().build();;
    private RpcController clientController;
    private Rpc.RpcControl failureResult =
            Rpc.RpcControl.newBuilder()
            .setMessageType(Rpc.MessageType.RPC_RETURNVALUE)
            .setStat(Rpc.StatusCode.HANDLER_FAILURE)
            .build();
    RpcMessageListener rpcMessageListener = new RpcMessageListener() {
        public void receiveMessage(
                final Object message,
                final ChannelHandlerContext ctx) {
            log.info("message = " + message);
        }
    };
    private Lock lock;
    private Condition resultReceived;
    private RpcController servingController;

    private final void signalResultReceived() {
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

        final RpcExecutionService snapdoll = new RpcExecutionServiceImpl();

        final RpcClient client = RpcSetup.setUpClient(HOST, port, snapdoll);
        RpcSetup.setUpServer(port, snapdoll, client, rpcMessageListener);

        client.start();

        clientChannel   = client.newClientRpcChannel();
        clientController = client.newController(clientChannel);
    }
    @Mock
    Receiver<String> callbackResponse;

    @Test
    public void testRpcInvocation() throws InterruptedException {

        final RpcCallback<Rpc.RpcResult> callback =
                new RpcCallback<Rpc.RpcResult>() {
                    public void run(final Rpc.RpcResult response) {
                        callbackResponse.receive(response.getReturnvalue());
                        signalResultReceived();
                    }
                };

        final Rpc.RpcService myService = Rpc.RpcService.newStub(clientChannel);
        myService.invoke(clientController, request, callback);

        try {
            lock.lock();
            log.info("Awaiting result received.");
            resultReceived.await();
        }
        finally {
            lock.unlock();
            log.info("unlocked, test passed");
        }

        verify(callbackResponse).receive(SampleServerImpl.RETURN_VALUE);
    }
}
