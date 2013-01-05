package no.rmz.mvp.hello;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcChannel;
import com.google.protobuf.RpcController;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;
import no.rmz.blobee.SampleServerImpl;
import no.rmz.blobee.rpc.RpcClient;
import no.rmz.blobee.rpc.RpcSetup;
import no.rmz.blobeeproto.api.proto.Rpc;
import no.rmz.testtools.Net;
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

    @Before
    public void setUp() throws
            NoSuchMethodException,
            IllegalAccessException,
            IllegalArgumentException,
            InvocationTargetException,
            IOException {
        port = Net.getFreePort();

        RpcSetup.setUpServer(port);
        RpcClient client = RpcSetup.setUpClient(HOST, port);

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
    public void testRpcInvocation() {
        /**
         * XXX Use this instead?
         * final Descriptors.ServiceDescriptor descriptor;
         * descriptor = Rpc.RpcService.getDescriptor();
         */

        final RpcCallback<Rpc.RpcResult> callback =
                new RpcCallback<Rpc.RpcResult>() {
                    public void run(final Rpc.RpcResult response) {
                        callbackResponse.receive(response.getReturnvalue());
                    }
                };

        final Rpc.RpcService myService = Rpc.RpcService.newStub(rchannel);
        myService.invoke(controller, request, callback);
        
        // XXX Eventually we'll enable this again
        // verify(callbackResponse).receive(SampleServerImpl.RETURN_VALUE);
    }
}
