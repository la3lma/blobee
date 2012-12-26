package no.rmz.blobee;

import com.google.common.base.Function;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Descriptors.ServiceDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
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
public final class SimpleInvocationLab {

    private final static Logger log = Logger.getLogger(SimpleInvocationLab.class.getName());

    private RChannel rchannel;

    @Before
    public void setUp() {
         rchannel = new RChannel();

         final SampleServerImpl ssi = new SampleServerImpl();

         // XXX This cruft could be done using annotations
        final MethodDescriptor methodDesc = Rpc.RpcService.getDescriptor().findMethodByName("Invoke");
        rchannel.add(methodDesc, new Function<Message, Message>() {

            public Message apply(final Message input) {
                // XXX This input isn't being cast to the proper type, that's
                //     probably a mistake.   However, that can quite easily
                //     be amended by clever setup using annotations.  Yeeha, this is
                //     doable and may in fact be quite nice to use ;)
                return ssi.invoke((Rpc.RpcParam) input);
            }
        });
    }

    @Test
    public void testRpcStuff() {
        final ServiceDescriptor descriptor = Rpc.RpcService.getDescriptor();


        final RpcController controller = rchannel.newController();

        final RpcCallback<RpcResult> rpcCallback = new RpcCallback<RpcResult>() {
            public void run(final RpcResult response) {
                if (response != null) {
                    log.info("The answer is: " + response);
                } else {
                    log.info("Oops, there was an error: "
                             + controller.errorText());
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
