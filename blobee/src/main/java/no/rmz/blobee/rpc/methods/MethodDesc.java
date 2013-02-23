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

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.MessageLite;


/**
 * Value object used to describe messages associated with
 * RPC APIs.
 */
public final class MethodDesc {

    private MethodDescriptor descriptor;
    private MessageLite parameterType;
    private MessageLite returnValueType;

    /**
     * Construct a new method descriptor.
     * @param descriptor The method descriptor, from protoc.
     * @param parameterType The
     * @param returnValueTupe
     */
    public MethodDesc(
            final MethodDescriptor descriptor,
            final MessageLite parameterType,
            final MessageLite outputType) {
        this.descriptor = checkNotNull(descriptor);
        this.parameterType = checkNotNull(parameterType);
        this.returnValueType = checkNotNull(outputType);
    }

    public MethodDescriptor getDescriptor() {
        return descriptor;
    }

    public MessageLite getInputType() {
        return parameterType;
    }

    public MessageLite getOutputType() {
        return returnValueType;
    }
}
