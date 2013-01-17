package no.rmz.blobee.rpc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)

@Deprecated
public @interface ProtobufRpcImplementation {
   Class  serviceClass();
   String method();
}