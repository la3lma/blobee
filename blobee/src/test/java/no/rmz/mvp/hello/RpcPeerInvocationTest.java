package no.rmz.mvp.hello;

import no.rmz.testtools.Receiver;
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
import no.rmz.blobee.rpc.RpcPeerHandler.DecodingContext;
import no.rmz.blobee.rpc.RpcSetup;
import no.rmz.blobeeproto.api.proto.Rpc;
import no.rmz.testtools.Net;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

// TODO:
//       o Implementer en ny rpc channel for en klient og bygg den opp
//         med enhetstester basert på probing inn i både server og klientene
//       o Utvid til roundtrip er oppnådd, vi har da en happy-day implementasjon
//         som kan funke som basis for diskusjon om sluttmålet :-)

@RunWith(MockitoJUnitRunner.class)
public final class RpcPeerInvocationTest {

    private static final Logger log = Logger.getLogger(
            no.rmz.mvp.hello.RpcPeerInvocationTest.class.getName());
    private final static String HOST = "localhost";
    private final static Rpc.RpcControl HEARTBEAT_MESSAGE =
            Rpc.RpcControl.newBuilder().setMessageType(Rpc.MessageType.HEARTBEAT).build();
    private final static Rpc.RpcControl SHUTDOWN_MESSAGE =
            Rpc.RpcControl.newBuilder().setMessageType(Rpc.MessageType.SHUTDOWN).build();
    private int port;
    private RpcChannel rchannel;
    private Rpc.RpcParam request;
    private RpcController controller;
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

   private  Lock lock;
   private  Condition resultReceived;

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


        final RpcExecutionService executor = new RpcExecutionService() {
            @Override
            public void execute(DecodingContext dc, ChannelHandlerContext ctx, Object param) {
                log.info("Executing dc = " + dc + ", param = " + param);

                try {
                    lock.lock();
                    resultReceived.signal();
                } finally {
                    lock.unlock();
                }
            }
        };



        RpcSetup.setUpServer(port, executor, rpcMessageListener);
        RpcClient client = RpcSetup.setUpClient(HOST, port, executor);

        rchannel   = client.newClientRpcChannel();
        controller = client.newController(rchannel);

        request = Rpc.RpcParam.newBuilder().build();

        // No server at the other end here, everything will
        // just get lost, but if we can create the correct parameters
        // that will be a long step in the right direction.
    }

    @Mock
    Receiver<String> callbackResponse;

    @Test
    public void testRpcInvocation() throws InterruptedException {

        /**
         * XXX Use this instead?
         * final Descriptors.ServiceDescriptor descriptor;
         * descriptor = Rpc.RpcService.getDescriptor();
         */


        final RpcCallback<Rpc.RpcResult> callback =
                new RpcCallback<Rpc.RpcResult>() {
                    public void run(final Rpc.RpcResult response) {
                        try {
                            lock.lock();
                            callbackResponse.receive(response.getReturnvalue());
                            resultReceived.signal();
                        }
                        finally {
                            lock.unlock();
                        }
                    }
                };

        final Rpc.RpcService myService = Rpc.RpcService.newStub(rchannel);
        myService.invoke(controller, request, callback);

        try {
            lock.lock();
            log.info("Awaiting result received.");
            resultReceived.await();
        } finally {
            lock.unlock();
            log.info("unlocked, test passed");
        }

        // XXX Eventually we'll enable this again
        // verify(callbackResponse).receive(SampleServerImpl.RETURN_VALUE);
    }
}
