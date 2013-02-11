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

package no.rmz.blobee.rpc.client;

import com.google.common.base.Function;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.MessageLite;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import no.rmz.blobeeproto.api.proto.Rpc;
import no.rmz.blobeeproto.api.proto.Rpc.MethodSignature;

// XXX This is one mean, ugly class.

// XXXX This class the main focus of the refactoring effort.

public final class ResolverImpl implements MethodSignatureResolver{


    private final Object monitor = new Object();



    private final Map<MethodDescriptor, Function<Message, Message>> methodsByMethodDescriptor =
            new HashMap<MethodDescriptor, Function<Message, Message>>();


    // XX Use this class instead of the cruft below.
     public final static class  MethodDesc {
         private Function<Message, Message>  function;
         private MethodDescriptor descriptor;
         private MessageLite inputType;
         private MessageLite outputType;

        public MethodDesc(
                final Function<Message, Message> function,
                final MethodDescriptor descriptor,
                final MessageLite inputType,
                final MessageLite outputType) {
            this.function =  function;
            this.descriptor = checkNotNull(descriptor);
            this.inputType = checkNotNull(inputType);
            this.outputType = checkNotNull(outputType);
        }

     }

     private final Map<MethodSignature, MethodDesc> mmap =
             new ConcurrentHashMap<MethodSignature, MethodDesc>();


     private final Map<MethodSignature, Function<Message, Message>> methodsByMethodSignature =
            new HashMap<MethodSignature, Function<Message, Message>>();
    private final Map<MethodSignature, MethodDescriptor> methodDescriptorByMethodSignature =
            new HashMap<MethodSignature, MethodDescriptor>();


    private final Map<MethodSignature, MessageLite>  inputTypes = new HashMap<MethodSignature, MessageLite>();
    private final Map<MethodSignature, MessageLite>  outputTypes = new HashMap<MethodSignature, MessageLite>();
    private final Map<MethodSignature, MethodDescriptor> sigToDesc = new HashMap<MethodSignature, MethodDescriptor>();

    public ResolverImpl() {
    }

    public MessageLite getPrototypeForParameter(final MethodSignature methodSignature) {
        checkNotNull(methodSignature);
        return inputTypes.get(methodSignature);
    }

    public MessageLite getPrototypeForReturnValue(MethodSignature methodSignature) {
        checkNotNull(methodSignature);
        return outputTypes.get(methodSignature);
    }


    public void addTypes(final MethodDescriptor md, MessageLite inputType, MessageLite outputType) {
        final MethodSignature ms = getMethodSignatureFromMethodDescriptor(md);
        inputTypes.put(ms, inputType);
        outputTypes.put(ms, outputType);
        sigToDesc.put(ms, md);


        // XXX This will be the new stuff.
        final MethodDesc methodDesc = new MethodDesc(null, md, inputType, outputType);
        mmap.put(ms, methodDesc);
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
