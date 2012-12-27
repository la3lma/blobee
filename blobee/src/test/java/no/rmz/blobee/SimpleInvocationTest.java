package no.rmz.blobee;

import com.google.protobuf.Descriptors.ServiceDescriptor;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import com.google.protobuf.ServiceException;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;
import no.rmz.blobeeproto.api.proto.Rpc;
import no.rmz.blobeeproto.api.proto.Rpc.RpcParam;
import no.rmz.blobeeproto.api.proto.Rpc.RpcResult;
import no.rmz.blobeeproto.api.proto.Rpc.RpcService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * The objective of the tests in this class is to get a grip on the mechanisms
 * for RPC management that are provided by the open source Protobuffer library
 * from Google. It's a purely experimental exercise and is not ye connected to
 * any library code for doing RPC. This is a strong inspiration:
 * http://code.google.com/p/protobuf-socket-rpc/wiki/JavaUsage
 */
public final class SimpleInvocationTest {

    private final static Logger log = Logger.getLogger(SimpleInvocationTest.class.getName());
    private RChannel rchannel;
    private RpcParam request;
    private boolean callbackWasCalled;
    private RpcController controller;
    private RpcResult failureResult =
            RpcResult.newBuilder().setStat(Rpc.StatusCode.HANDLER_FAILURE).build();

    @Before
    public void setUp() throws
            NoSuchMethodException,
            IllegalAccessException,
            IllegalArgumentException,
            InvocationTargetException {
        rchannel = new RChannel();

        final SampleServerImpl implementation = new SampleServerImpl();
        ServiceAnnotationMapper.bindServices(implementation, rchannel);
        callbackWasCalled = false;
        request =
                Rpc.RpcParam.newBuilder().build();
        controller = rchannel.newController();
    }

    @After
    public void postConditions() {
        org.junit.Assert.assertFalse(
                String.format("Rpc failed %s ", controller.errorText()),
                controller.failed());
    }

    @Test
    public void testBasicNonblockingRpc() {
        final ServiceDescriptor descriptor = Rpc.RpcService.getDescriptor();

        final RpcController controller = rchannel.newController();

        final RpcCallback<RpcResult> callback = new RpcCallback<RpcResult>() {
            public void run(final RpcResult response) {
                callbackWasCalled = true;
                if (response != null) {
                    org.junit.Assert.assertEquals(failureResult, response);
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
        final Rpc.RpcService.BlockingInterface service;
        service = Rpc.RpcService.newBlockingStub(rchannel.newBlockingRchannel());

        final RpcResult response = service.invoke(controller, request);
        org.junit.Assert.assertEquals(failureResult, response);
    }
}
