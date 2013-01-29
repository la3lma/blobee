package no.rmz.blobee.rpc;

import com.google.protobuf.MessageLite;
import no.rmz.blobeeproto.api.proto.Rpc.MethodSignature;

public interface MethodSignatureResolver {

    MessageLite getPrototypeForParameter(final MethodSignature methodSignature);

    MessageLite getPrototypeForReturnValue(final MethodSignature methodSignature);

}
