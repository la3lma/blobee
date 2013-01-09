package no.rmz.blobee.rpc;

import com.google.common.base.Function;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Message;
import java.util.HashMap;


public final class MethodMap {


    private final HashMap<MethodDescriptor, Function<Message, Message>> methods =
            new HashMap<MethodDescriptor, Function<Message, Message>>();

    public MethodMap() {
    }


    public void add(
            final MethodDescriptor key,
            final Function<Message, Message> function) {
        // XXX No synchronization or anything here.
        checkNotNull(key);
        checkNotNull(function);
        synchronized (methods) {
            methods.put(key, function);
        }
    }

    public Function<Message, Message> get(final MethodDescriptor key) {
        checkNotNull(key);
        synchronized (methods) {
            return methods.get(key);
        }
    }
}
