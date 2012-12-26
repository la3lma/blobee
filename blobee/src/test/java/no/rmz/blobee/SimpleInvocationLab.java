package no.rmz.blobee;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Descriptors.ServiceDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcChannel;
import com.google.protobuf.RpcController;
import java.util.logging.Logger;
import no.rmz.blobeeproto.api.proto.Rpc;
import no.rmz.blobeeproto.api.proto.Rpc.RpcParam;
import no.rmz.blobeeproto.api.proto.Rpc.RpcResult;
import no.rmz.blobeeproto.api.proto.Rpc.RpcService;
import org.junit.Test;

/**
 * The objective of the tests in this class is to get a grip on the mechanisms
 * for RPC management that are provided by the open source Protobuffer library
 * from Google. It's a purely experimental exercise and is not ye connected to
 * any library code for doing RPC.
 */
public final class SimpleInvocationLab {

    private final static Logger log = Logger.getLogger(SimpleInvocationLab.class.getName());

    // Take a long hard look at http://code.google.com/p/netty-protobuf-rpc/wiki/Usage

    @Test
    public void testRpcStuff() {
        final ServiceDescriptor descriptor = Rpc.RpcService.getDescriptor();
        log.info("Descriptor.fullname = " + descriptor.getFullName());


        final RpcChannel rchannel = new RpcChannel() {
            public void callMethod(
                    final MethodDescriptor method,
                    final RpcController controller,
                    final Message request,
                    final Message responsePrototype,
                    final RpcCallback<Message> callback) {
                checkNotNull(method);
                checkNotNull(controller);
                checkNotNull(request);
                checkNotNull(responsePrototype);
                checkNotNull(callback);
                final Message message =
                        RpcResult.newBuilder().setStat(Rpc.StatusCode.HANDLER_FAILURE).build();

                callback.run(message);
            }
        };


        final RpcController controller = new RpcController() {
            public void reset() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public boolean failed() {
                return false;
            }

            public String errorText() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public void startCancel() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public void setFailed(String reason) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public boolean isCanceled() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public void notifyOnCancel(RpcCallback<Object> callback) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        };


        RpcCallback<RpcResult> rpcCallback = new RpcCallback<RpcResult>() {
            public void run(final RpcResult response) {
                if (response != null) {
                    log.info("The answer is: " + response);
                } else {
                    log.info(
                            "Oops, there was an error: "
                            + controller.errorText());
                }
            }
        };

        // There is an example in
        // From http://code.google.com/p/protobuf-socket-rpc/wiki/JavaUsage

        // Call service asynchronously.
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
