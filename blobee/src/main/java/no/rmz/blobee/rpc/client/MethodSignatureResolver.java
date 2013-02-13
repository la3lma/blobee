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

package no.rmz.blobee.rpc.client;

import com.google.protobuf.Descriptors;
import com.google.protobuf.MessageLite;
import no.rmz.blobeeproto.api.proto.Rpc.MethodSignature;

public interface MethodSignatureResolver {

    MessageLite getPrototypeForParameter(
            final MethodSignature methodSignature);

    MessageLite getPrototypeForReturnValue(
            final MethodSignature methodSignature);

    void addTypes(
            final Descriptors.MethodDescriptor md,
            final MessageLite inputType,
            final MessageLite outputType);
}
