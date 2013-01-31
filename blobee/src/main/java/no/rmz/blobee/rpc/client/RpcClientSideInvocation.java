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

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import no.rmz.blobee.controllers.RpcClientController;
import no.rmz.blobee.controllers.RpcClientControllerImpl;


public final class RpcClientSideInvocation {
    private final MethodDescriptor method;
    private final RpcClientController controller;
    private final Message request;
    private final Message responsePrototype;
    private final RpcCallback<Message> done;

    public RpcClientSideInvocation(
            final MethodDescriptor method,
            final RpcController controller,
            final Message request,
            final Message responsePrototype,
            final RpcCallback<Message> done) {

        this.method = checkNotNull(method);

        final RpcClientControllerImpl rcci =
                (RpcClientControllerImpl) controller;
        this.controller = checkNotNull(rcci);
        rcci.bindToInvocation(this);

        this.request = checkNotNull(request);
        this.responsePrototype = checkNotNull(responsePrototype);
        this.done = checkNotNull(done);
    }

    public MethodDescriptor getMethod() {
        return method;
    }

    public RpcClientController getController() {
        return controller;
    }

    public Message getRequest() {
        return request;
    }

    public Message getResponsePrototype() {
        return responsePrototype;
    }

    public RpcCallback<Message> getDone() {
        return done;
    }
}
