package no.rmz.blobee;

import com.google.protobuf.Descriptors.ServiceDescriptor;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import com.google.protobuf.ServiceException;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;
import no.rmz.blobee.rpc.MethodMap;
import no.rmz.blobee.rpc.ServiceAnnotationMapper;
import no.rmz.blobee.rpc.ServingRpcChannel;
import no.rmz.blobeeproto.api.proto.Rpc;
import no.rmz.blobeeproto.api.proto.Rpc.RpcControl;
import no.rmz.blobeeprototest.api.proto.Testservice;
import no.rmz.blobeeprototest.api.proto.Testservice.RpcParam;
import no.rmz.blobeeprototest.api.proto.Testservice.RpcResult;
import no.rmz.blobeeprototest.api.proto.Testservice.RpcService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * The objective of the tests in this class is to get a grip on the mechanisms
 * for RPC management that are provided by the open source Protobuffer library
 * from Google. It's a purely experimental exercise and is not yet connected to
 * any library code for doing RPC. This is a strong inspiration:
 * http://code.google.com/p/protobuf-socket-rpc/wiki/JavaUsage
 */
public final class SimpleInvocationTest {

    private final static Logger log = Logger.getLogger(SimpleInvocationTest.class.getName());
    private ServingRpcChannel rchannel;
    private RpcParam request;
    private boolean callbackWasCalled;
    private RpcController controller;
    private RpcControl failureResult =
            RpcControl.newBuilder()
            .setMessageType(Rpc.MessageType.RPC_RETURNVALUE)
            .setStat(Rpc.StatusCode.HANDLER_FAILURE)
            .build();

    @Before
    public void setUp() throws
            NoSuchMethodException,
            IllegalAccessException,
            IllegalArgumentException,
            InvocationTargetException {
        final MethodMap methodMap = new MethodMap();
        rchannel = new ServingRpcChannel(methodMap);
        final SampleServerImpl1 implementation = new SampleServerImpl1();
        ServiceAnnotationMapper.bindServices(implementation, methodMap);
        callbackWasCalled = false;
        request =  RpcParam.newBuilder().build();
        controller = rchannel.newController();
    }

    @After
    public void postConditions() {
        org.junit.Assert.assertNotNull(controller);
        org.junit.Assert.assertFalse(
                String.format("Rpc failed %s ", controller.errorText()),
                controller.failed());
    }

    @Test
    public void testBasicNonblockingRpc() {
        final ServiceDescriptor descriptor;
        descriptor = Testservice.RpcService.getDescriptor();

        final RpcCallback<RpcResult> callback = new RpcCallback<RpcResult>() {
            public void run(final RpcResult response) {
                callbackWasCalled = true;
                if (response != null) {
                    org.junit.Assert.assertEquals(
                            SampleServerImpl1.RETURN_VALUE, response.getReturnvalue());
                } else {
                    org.junit.Assert.fail("Badness: " + controller.errorText());
                }
            }
        };

        final RpcService myService = RpcService.newStub(rchannel);

        myService.invoke(controller, request, callback);
        org.junit.Assert.assertTrue(callbackWasCalled);
    }

    @Test
    public void testBasicBlockingRpc() throws ServiceException {
        final Testservice.RpcService.BlockingInterface service;
        service = Testservice.RpcService.newBlockingStub(rchannel.newBlockingRchannel());

        final RpcResult response = service.invoke(controller, request);
        org.junit.Assert.assertEquals(SampleServerImpl1.RETURN_VALUE, response.getReturnvalue());
    }
}
