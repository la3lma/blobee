/**
 * Copyright 2013  Bjørn Remseth (la3lma@gmail.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package no.rmz.blobee.rpc;

import com.google.common.base.Function;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.MessageLite;
import java.util.HashMap;
import java.util.Map;
import no.rmz.blobeeproto.api.proto.Rpc;
import no.rmz.blobeeproto.api.proto.Rpc.MethodSignature;

public final class MethodMap implements MethodSignatureResolver{

    private final Map<MethodDescriptor, Function<Message, Message>> methodsByMethodDescriptor =
            new HashMap<MethodDescriptor, Function<Message, Message>>();
    private final Map<MethodSignature, Function<Message, Message>> methodsByMethodSignature =
            new HashMap<Rpc.MethodSignature, Function<Message, Message>>();
    private final Map<MethodSignature, MethodDescriptor> methodDescriptorByMethodSignature =
            new HashMap<MethodSignature, MethodDescriptor>();
    private final Object monitor = new Object();

    private final Map<MethodSignature, MessageLite>  inputTypes = new HashMap<MethodSignature, MessageLite>();
    private final Map<MethodSignature, MessageLite>  outputTypes = new HashMap<MethodSignature, MessageLite>();
    private final Map<MethodSignature, MethodDescriptor> sigToDesc = new HashMap<MethodSignature, MethodDescriptor>();

    public MethodMap() {
    }

    public MessageLite getPrototypeForParameter(final MethodSignature methodSignature) {
        checkNotNull(methodSignature);
        return inputTypes.get(methodSignature);
    }

    public MessageLite getPrototypeForReturnValue(MethodSignature methodSignature) {
        checkNotNull(methodSignature);
        return outputTypes.get(methodSignature);
    }


    void addTypes(final MethodDescriptor md, MessageLite inputType, MessageLite outputType) {
        final MethodSignature ms = getMethodSignatureFromMethodDescriptor(md);
        inputTypes.put(ms, inputType);
        outputTypes.put(ms, outputType);
        sigToDesc.put(ms, md);
    }

    public static MethodSignature getMethodSignatureFromMethodDescriptor(
            final MethodDescriptor key) {
         checkNotNull(key);
          final MethodSignature signature =
                    MethodSignature.newBuilder()
                    .setInputType(key.getInputType().getFullName().toString())
                    .setMethodName(key.getFullName())
                    .setOutputType(key.getOutputType().getFullName().toString())
                    .build();
          return signature;
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
                    getMethodSignatureFromMethodDescriptor(key);
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
