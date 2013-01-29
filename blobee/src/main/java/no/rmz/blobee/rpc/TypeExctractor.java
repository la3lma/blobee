package no.rmz.blobee.rpc;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Message;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import no.rmz.blobee.rpc.MethodTypeException;

public final class TypeExctractor {
    public static final String GET_RESPONSE_PROTOTYPE_METHODNAME = "getResponsePrototype";
    public static final String GET_REQUEST_PROTOTYPE_METHODNAME = "getRequestPrototype";

    public static Message getReqestPrototype(final Object instance, final MethodDescriptor md) throws MethodTypeException {
        final Method method = findMethod(instance.getClass(), GET_REQUEST_PROTOTYPE_METHODNAME);
        return applyMethodToMethodDescriptor(instance, method, md);
    }

    public static Message getResponsePrototype(final Object instance, final MethodDescriptor md) throws MethodTypeException {
        final Method method = findMethod(instance.getClass(), GET_RESPONSE_PROTOTYPE_METHODNAME);
        return applyMethodToMethodDescriptor(instance, method, md);
    }

    public static Method findMethod(final Class clazz, final String methodName) {
        checkNotNull(methodName);
        checkNotNull(clazz);
        return findMethod(clazz.getMethods(), methodName);
    }

    public static Method findMethod(final Method[] methods, final String methodName) {
        checkNotNull(methods);
        checkNotNull(methodName);

        for (final Method m : methods) {
            if (m.getName().equals(methodName)) {
                return m;
            }
        }
        return null;
    }

    public static Message applyMethodToMethodDescriptor(final Object instance, final Method method, final MethodDescriptor md) throws MethodTypeException {
        checkNotNull(instance);
        checkNotNull(method);
        checkNotNull(md);
        try {
            return (Message) method.invoke(instance, md);
        }
        catch (IllegalAccessException ex) {
            throw new MethodTypeException(ex);
        }
        catch (IllegalArgumentException ex) {
            throw new MethodTypeException(ex);
        }
        catch (InvocationTargetException ex) {
            throw new MethodTypeException(ex);
        }
    }
}
