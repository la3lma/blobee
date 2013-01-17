package no.rmz.blobee.rpc;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Descriptors.ServiceDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Logger;

public final class ServiceAnnotationMapper {

    private static final Logger log =
            Logger.getLogger(ServiceAnnotationMapper.class.getName());

    private ServiceAnnotationMapper() {
    }

    public static MethodDescriptor getMethodDescriptor(
            final Class serviceInterface,
            final String methodName) throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        checkNotNull(methodName);
        checkNotNull(serviceInterface);


        final Method getDescriptor = serviceInterface.getMethod("getDescriptor");
        final ServiceDescriptor serviceDescriptor =
                (ServiceDescriptor) getDescriptor.invoke(null);
        final MethodDescriptor methodDesc =
                serviceDescriptor.findMethodByName(methodName);
        return methodDesc;
    }
}
