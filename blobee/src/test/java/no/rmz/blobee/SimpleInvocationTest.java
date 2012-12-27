package no.rmz.blobee;

import com.google.common.base.Function;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Descriptors.ServiceDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import no.rmz.blobeeproto.api.proto.Rpc;
import no.rmz.blobeeproto.api.proto.Rpc.RpcParam;
import no.rmz.blobeeproto.api.proto.Rpc.RpcResult;
import no.rmz.blobeeproto.api.proto.Rpc.RpcService;
import org.junit.Before;
import org.junit.Test;

/**
 * The objective of the tests in this class is to get a grip on the mechanisms
 * for RPC management that are provided by the open source Protobuffer library
 * from Google. It's a purely experimental exercise and is not ye connected to
 * any library code for doing RPC.
 */
public final class SimpleInvocationTest {

    private final static Logger log = Logger.getLogger(SimpleInvocationTest.class.getName());
    private RChannel rchannel;

    @Before
    public void setUp() throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        rchannel = new RChannel();

        final SampleServerImpl ssi = new SampleServerImpl();

        for (final Method method : ssi.getClass().getMethods()) {
            if (method.isAnnotationPresent(ProtobufRpcImplementation.class)) {
                final ProtobufRpcImplementation annotation =
                        method.getAnnotation(ProtobufRpcImplementation.class);

                // Then find the MethodDescriptor for the
                // service class

                final Class serviceClass = annotation.serviceClass();
                final Method getDescriptor = serviceClass.getMethod("getDescriptor");
                final ServiceDescriptor serviceDescriptor = (ServiceDescriptor) getDescriptor.invoke(null);
                final MethodDescriptor methodDesc = serviceDescriptor.findMethodByName(annotation.method());

                // Then use that method descriptor to create a proper wrapper
                // for the implementation.

                final Function<Message, Message> wrapper =
                        new Function<Message, Message> () {
                        @Override
                        public Message apply(final Message input) {
                                try {
                                    return (Message) method.invoke(ssi, input);
                                }
                                catch (IllegalAccessException ex) {
                                    log.log(Level.SEVERE, null, ex);
                                }
                                catch (IllegalArgumentException ex) {
                                    log.log(Level.SEVERE, null, ex);
                                }
                                catch (InvocationTargetException ex) {
                                    log.log(Level.SEVERE, null, ex);
                                }
                                throw new RuntimeException("This is bad");
                         }
                     };

                rchannel.add(methodDesc, wrapper);
            }
        }
    }

    @Test
        public void testRpcStuff() {
        final ServiceDescriptor descriptor = Rpc.RpcService.getDescriptor();


        final RpcController controller = rchannel.newController();

        final RpcCallback<RpcResult> rpcCallback = new RpcCallback<RpcResult>() {
            public void run(final RpcResult response) {
                if (response != null) {
                    log.info("The answer is: " + response);
                    org.junit.Assert.assertEquals(Rpc.StatusCode.HANDLER_FAILURE, response);
                } else {
                    log.info("Oops, there was an error: "
                             + controller.errorText());
                    org.junit.Assert.fail("There shouldn't be any errors");
                }
            }
        };

        // There is an example in
        // From http://code.google.com/p/protobuf-socket-rpc/wiki/JavaUsage


        final RpcService myService = RpcService.newStub(rchannel);
        final RpcParam request =
                Rpc.RpcParam.newBuilder().build();


        final RpcCallback<Rpc.RpcResult> callback =
                new RpcCallback<Rpc.RpcResult>() {
                    public void run(Rpc.RpcResult myResponse) {
                        log.info("Received Response: " + myResponse);
                    }
                };

        // Do the actual invocation
        myService.invoke(controller, request, callback);

        // Measure the fallout
        if (controller.failed()) {
            log.info(String.format("Rpc failed %s ",
                    controller.errorText()));
        }
    }
}
