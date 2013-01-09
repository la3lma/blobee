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
import no.rmz.blobee.rpc.MethodMap;
import no.rmz.blobee.rpc.RemoteExecutionContext;
import no.rmz.blobee.rpc.RpcClient;
import no.rmz.blobee.rpc.RpcExecutionService;
import no.rmz.blobee.rpc.RpcMessageListener;
import no.rmz.blobee.rpc.RpcSetup;
import no.rmz.blobee.rpc.ServiceAnnotationMapper;
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

    public final static class Foo implements RpcExecutionService {

        private static final Logger log = Logger.getLogger(
                Foo.class.getName());
        private Rpc.RpcParam request;
        final ServingRpcChannel servingChannel;
        final Rpc.RpcService myService;

        public Foo() throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
            final MethodMap methodMap = new MethodMap();
            servingChannel = new ServingRpcChannel(methodMap);

            final SampleServerImpl implementation = new SampleServerImpl();
            ServiceAnnotationMapper.bindServices(implementation, methodMap);

            request = Rpc.RpcParam.newBuilder().build();


            // XXX Could presumably be reused
            myService = Rpc.RpcService.newStub(servingChannel);

        }

        @Override
        public void execute(
                final RemoteExecutionContext dc,
                final ChannelHandlerContext ctx,
                final Object param) {
            log.info("Executing dc = " + dc + ", param = " + param);

            final RpcCallback<Rpc.RpcResult> callback = new RpcCallback<Rpc.RpcResult>() {
                public void run(final Rpc.RpcResult response) {
                    dc.returnResult(response);
                }
            };

            RpcController servingController;
            servingController = servingChannel.newController();
            myService.invoke(servingController, request, callback);
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

        final MethodMap methodMap = new MethodMap();
        servingChannel = new ServingRpcChannel(methodMap);

        final SampleServerImpl implementation = new SampleServerImpl();
        ServiceAnnotationMapper.bindServices(implementation, methodMap);

        request = Rpc.RpcParam.newBuilder().build();


        // XXX Could presumably be reused
        final Rpc.RpcService myService = Rpc.RpcService.newStub(servingChannel);

        final RpcExecutionService executor;
        executor = new RpcExecutionService() {
            @Override
            public void execute(
                    final RemoteExecutionContext dc,
                    final ChannelHandlerContext ctx,
                    final Object param) {
                log.info("Executing dc = " + dc + ", param = " + param);

                final RpcCallback<Rpc.RpcResult> callback = new RpcCallback<Rpc.RpcResult>() {
                    public void run(final Rpc.RpcResult response) {
                        dc.returnResult(response);
                    }
                };

                servingController = servingChannel.newController();
                myService.invoke(servingController, request, callback);
            }
        };

         final RpcExecutionService snapdoll = new Foo();


        final RpcClient client = RpcSetup.setUpClient(HOST, port, snapdoll);
        RpcSetup.setUpServer(port, snapdoll, client, rpcMessageListener);

        client.start();

        rchannel = client.newClientRpcChannel();
        controller = client.newController(rchannel);

        request = Rpc.RpcParam.newBuilder().build();
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

        final Rpc.RpcService myService = Rpc.RpcService.newStub(rchannel);
        myService.invoke(controller, request, callback);

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
