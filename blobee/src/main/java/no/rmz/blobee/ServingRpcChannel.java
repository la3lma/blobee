package no.rmz.blobee;

import com.google.common.base.Function;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.protobuf.BlockingRpcChannel;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcChannel;
import com.google.protobuf.RpcController;
import com.google.protobuf.ServiceException;
import java.util.HashMap;

/**
 * Experimental  implementation of an RpcChannel.  Used as a vehicle
 * to wrap my mind about the various details involved in making an RPC
 * library.
 */
public final class ServingRpcChannel implements RpcChannel {

    // XXX Much-too-generic mappers of input params to
    //     output params.
    final HashMap<MethodDescriptor,
            Function<Message, Message>> methods =
                new HashMap<MethodDescriptor, Function<Message, Message>>();


    @Override
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

        // XXX Happy day scenario. Must handle a lot more
        //     bogusness before it's believable :)
        final Function<Message, Message> meth;
        meth = methods.get(method);
        final Message result = meth.apply(request);
        callback.run(result);
    }

    public void add(
            final MethodDescriptor key,
            final Function<Message, Message> function) {
        // XXX No synchronization or anything here.
        checkNotNull(key);
        checkNotNull(function);
        methods.put(key, function);
    }

    // XXX So far veeery loosely coupled with anything else.
    public RpcController newController() {
        return new RpcController() {
            public void reset() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public boolean failed() {
                return false;
            }

            public String errorText() {
                return "";
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
    }

    // XXX This is a rather bogus api, refactor asap!!

    public BlockingRpcChannel newBlockingRchannel() {
        return new BlockingRpcChannel() {
            public Message callBlockingMethod(
                    final MethodDescriptor method,
                    final RpcController controller,
                    final Message request,
                    final Message responsePrototype) throws ServiceException {

                final Function<Message, Message> meth;
                meth = methods.get(method);
                final Message result = meth.apply(request);

                return result;
            }
        };
    }
}
