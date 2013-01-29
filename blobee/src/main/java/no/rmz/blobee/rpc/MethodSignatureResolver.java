package no.rmz.blobee.rpc;

import com.google.protobuf.Descriptors;
import com.google.protobuf.MessageLite;
import no.rmz.blobeeproto.api.proto.Rpc.MethodSignature;

public interface MethodSignatureResolver {

    MessageLite getPrototypeForParameter(final MethodSignature methodSignature);

    MessageLite getPrototypeForReturnValue(final MethodSignature methodSignature);

    // XXX ???
    void addTypes(final Descriptors.MethodDescriptor md, final MessageLite inputType, final MessageLite outputType);
}
