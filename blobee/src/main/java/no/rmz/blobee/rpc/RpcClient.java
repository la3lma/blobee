package no.rmz.blobee.rpc;

import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcChannel;
import com.google.protobuf.RpcController;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;
import no.rmz.blobeeproto.api.proto.Rpc;
import no.rmz.blobeeproto.api.proto.Rpc.MethodSignature;
import no.rmz.blobeeproto.api.proto.Rpc.RpcControl;
import org.jboss.netty.channel.Channel;

public final class RpcClient {
    private static final Logger log = Logger.getLogger(RpcClient.class.getName());
    private final int capacity;
    final BlockingQueue<RpcClientSideInvocation> incoming;

    private volatile boolean running = true;


    private final Map<Long, RpcClientSideInvocation> invocations =
            new TreeMap<Long, RpcClientSideInvocation>();


    private long nextIndex;

    final Runnable incomingDispatcher = new Runnable() {

        public void run() {
            while(running) {
                try {
                    // First we remember this invocation so we can
                    // get back to it later
                    final RpcClientSideInvocation invocation = incoming.take();
                    final Long  currentIndex = nextIndex++;
                    invocations.put(currentIndex, invocation);

                    // Then creating the protobuf representation of
                    // the invocation, in preparation of sending it
                    // down the wire.

                    final MethodDescriptor   md = invocation.getMethod();
                    final String     methodName = md.getFullName();
                    final String     inputType  = md.getInputType().getName();
                    final String     outputType = md.getOutputType().getName();

                    final MethodSignature ms = Rpc.MethodSignature.newBuilder()
                            .setMethodName(methodName)
                            .setInputType(inputType)
                            .setOutputType(outputType)
                            .build();

                    final RpcControl invocationControl =
                            Rpc.RpcControl.newBuilder()
                            .setMessageType(Rpc.MessageType.RPC_INVOCATION)
                            .setRpcIndex(currentIndex)
                            .setMethodSignature(ms)
                            .build();

                    // Then send the invocation down the wire.
                    // XXX This is a bug! Needs to be -strictly- serialized
                    //     so stronger serialization is required
                    //     on this operation!
                    channel.write(invocationControl);
                    channel.write(invocation.getRequest());
                }
                catch (InterruptedException ex) {
                    log.warning("Something went south");
                }
            }
        }
    };

    final Channel channel;
    final RpcPeerPipelineFactory clientPipelineFactory;

    public RpcClient(
            final int capacity,
            final Channel channel,
            final RpcPeerPipelineFactory clientPipelineFactory) {
        // XXX Checking of params
        this.capacity = capacity;
        this.incoming =
                new ArrayBlockingQueue<RpcClientSideInvocation>(capacity);
        this.channel = channel;
        this.clientPipelineFactory = clientPipelineFactory;

        final Thread dispatcherThread =
                new Thread(incomingDispatcher, "Incoming dispatcher");
        dispatcherThread.start();
    }



    public RpcChannel newClientRpcChannel() {
        return new RpcChannel() {

            public void callMethod(
                    final MethodDescriptor method,
                    final RpcController controller,
                    final Message request,
                    final Message responsePrototype,
                    final RpcCallback<Message> done) {
                /// XXX CheckNotNull

                final RpcClientSideInvocation invocation =
                        new RpcClientSideInvocation(method, controller, request, responsePrototype, done);
                incoming.add(invocation);
            }
        };
    }

    public RpcController newController(final RpcChannel rchannel) {
         return new RpcController() {

            public void reset() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public boolean failed() {
                throw new UnsupportedOperationException("Not supported yet.");
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

            public void notifyOnCancel(final RpcCallback<Object> callback) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        };
    }
}