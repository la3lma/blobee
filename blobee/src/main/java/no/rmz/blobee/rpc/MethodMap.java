package no.rmz.blobee.rpc;

import com.google.common.base.Function;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Message;
import java.util.HashMap;
import java.util.Map;
import no.rmz.blobeeproto.api.proto.Rpc;
import no.rmz.blobeeproto.api.proto.Rpc.MethodSignature;

public final class MethodMap {

    private final Map<MethodDescriptor, Function<Message, Message>> methodsByMethodDescriptor =
            new HashMap<MethodDescriptor, Function<Message, Message>>();
    private final Map<MethodSignature, Function<Message, Message>> methodsByMethodSignature =
            new HashMap<Rpc.MethodSignature, Function<Message, Message>>();
    private final Map<MethodSignature, MethodDescriptor> methodDescriptorByMethodSignature =
            new HashMap<MethodSignature, MethodDescriptor>();
    private final Object monitor = new Object();

    public MethodMap() {
    }

    public void add(
            final MethodDescriptor key,
            final Function<Message, Message> function) {
        // XXX No synchronization or anything here.
        checkNotNull(key);
        checkNotNull(function);
        synchronized (monitor) {
            methodsByMethodDescriptor.put(key, function);
            final MethodSignature signature =
                    MethodSignature.newBuilder()
                    .setInputType(key.getInputType().getFullName().toString())
                    .setMethodName(key.getFullName())
                    .setOutputType(key.getOutputType().getFullName().toString())
                    .build();
            methodsByMethodSignature.put(signature, function);

            methodDescriptorByMethodSignature.put(signature, key);
        }
    }

    public Function<Message, Message> getByMethodDescriptor(
            final MethodDescriptor key) {
        checkNotNull(key);
        synchronized (monitor) {
            return methodsByMethodDescriptor.get(key);
        }
    }

    public Function<Message, Message> getByMethodSignature(
            final MethodSignature key) {
        checkNotNull(key);
        synchronized (monitor) {
            return methodsByMethodSignature.get(key);
        }
    }

    public MethodDescriptor getMethodDescriptorFromMethodSignature(
            final MethodSignature signature) {
        synchronized (monitor) {
            return methodDescriptorByMethodSignature.get(signature);
        }
    }
}
