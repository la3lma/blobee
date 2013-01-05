package no.rmz.blobee;

import com.google.common.base.Function;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Descriptors.ServiceDescriptor;
import com.google.protobuf.Message;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ServiceAnnotationMapper {
    private static final Logger log = Logger.getLogger(ServiceAnnotationMapper.class.getName());

    private ServiceAnnotationMapper() {
    }

    // XXX This is obviously just a stub.
    //     A lot more checking should be done here, including
    //     typechecking that is done at startup (semi-static ;-)

    public static void bindServices(
            final Object implementation,
            final ServingRpcChannel rchannel) throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        checkNotNull(implementation);
        for (final Method method : implementation.getClass().getMethods()) {
            if (method.isAnnotationPresent(ProtobufRpcImplementation.class)) {
                final ProtobufRpcImplementation annotation = method.getAnnotation(ProtobufRpcImplementation.class);
                // Then find the MethodDescriptor for the
                // service class
                final Class serviceClass = annotation.serviceClass();
                final Method getDescriptor = serviceClass.getMethod("getDescriptor");
                final ServiceDescriptor serviceDescriptor = (ServiceDescriptor) getDescriptor.invoke(null);
                final MethodDescriptor methodDesc = serviceDescriptor.findMethodByName(annotation.method());
                // Then use that method descriptor to create a proper wrapper
                // for the implementation.
                final Function<Message, Message> wrapper = new Function<Message, Message>() {
                    @Override
                    public Message apply(final Message input) {
                        try {
                            return (Message) method.invoke(implementation, input);
                        }
                        catch (IllegalAccessException ex) {
                            log.log(Level.SEVERE, null, ex);
                        }
                        catch (IllegalArgumentException ex) {
                            log.log(Level.SEVERE, null, ex);
                        }
                        catch (InvocationTargetException ex) {
                            log.log(Level.SEVERE, null, ex);
                        }
                        throw new RuntimeException("This is bad");
                    }
                };

                // XXX Duplicates, locking and all the rest are
                //     ignored here.
                // Finally add the new implementation
                rchannel.add(methodDesc, wrapper);
            }
        }
    }
}
