package no.rmz.mvp.hello;

import com.google.protobuf.Descriptors;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;
import no.rmz.blobee.SampleServerImpl;
import no.rmz.blobee.ServiceAnnotationMapper;
import no.rmz.blobee.ServingRpcChannel;
import no.rmz.blobeeproto.api.proto.Rpc;
import no.rmz.testtools.Net;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

// TODO:
//       o Få testen til å passere _uten_ å skru på serverene (essensielt,
//         replikering av testen i SampleServerImpl.
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
    private ServingRpcChannel rchannel;
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


        /*
         * XXX This will of course have to enabled
         RpcSetup.setUpServer(port, ml);
         final RpcClient client = RpcSetup.setUpClient(HOST, port, ml);
         rchannel = client.newClientRpcChannel();
         */


        rchannel = new ServingRpcChannel();

        final SampleServerImpl implementation = new SampleServerImpl();
        ServiceAnnotationMapper.bindServices(implementation, rchannel);
        request = Rpc.RpcParam.newBuilder().build();
        controller = rchannel.newController();
    }
    @Mock
    Receiver<String> callbackResponse;

    @Test
    public void testRpcInvocation() {


        final Descriptors.ServiceDescriptor descriptor;
        descriptor = Rpc.RpcService.getDescriptor();

        final RpcCallback<Rpc.RpcResult> callback =
                new RpcCallback<Rpc.RpcResult>() {
                    public void run(final Rpc.RpcResult response) {
                        callbackResponse.receive(response.getReturnvalue());
                    }
                };

        final Rpc.RpcService myService = Rpc.RpcService.newStub(rchannel);

        myService.invoke(controller, request, callback);

        verify(callbackResponse).receive(SampleServerImpl.RETURN_VALUE);
    }
}
