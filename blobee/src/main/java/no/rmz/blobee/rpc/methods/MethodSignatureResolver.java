/**
 * Copyright 2013 Bjørn Remseth (la3lma@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package no.rmz.blobee.rpc.methods;

import com.google.protobuf.Descriptors;
import com.google.protobuf.MessageLite;
import no.rmz.blobeeproto.api.proto.Rpc.MethodSignature;

/**
 * An interface  used to resolve method signatures, and to store them when
 * they are extracted from the client implementation.
 */
public interface MethodSignatureResolver {

    /**
     * Given a method signature (which is a protobuf-serializable
     * description of an RPC invoked procedure), return the prototype
     * instance for the parameter type.  This prototype can then
     * be used to deserialize an incoming protobuf packet for a method
     * invocation.
     * @param methodSignature A method signature describing which
     *     procedure this request is all about.
     * @return The prototype for the parameter for the procedure.
     */
    MessageLite getPrototypeForParameter(
            final MethodSignature methodSignature);


    /**
     * Given a method signature (which is a protobuf-serializable
     * description of an RPC invoked procedure), return the prototype
     * instance for the return type.  This prototype can then
     * be used to deserialize an incoming protobuf packet for a
     * returning method invocation.
     * @param methodSignature A method signature describing which
     *     procedure this request is all about.
     * @return The prototype for the return value for theprocedure.
     */
    MessageLite getPrototypeForReturnValue(
            final MethodSignature methodSignature);

    /**
     * Add a description for a method in an RPC callable
     * procedure.
     * @param md The method.
     * @param inputType The prototype for the parameter.
     * @param outputType  The prototype for the return value.
     */
    void addTypes(
            final Descriptors.MethodDescriptor md,
            final MessageLite inputType,
            final MessageLite outputType);
}
