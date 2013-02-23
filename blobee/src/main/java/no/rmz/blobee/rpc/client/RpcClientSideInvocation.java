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


/**
 * This is a value object that represents the client side of a
 * invocation.  It contains enough information to return the
 * results from an invocation to the invoker and its controller
 * instance.
 */
public final class RpcClientSideInvocation {

    private final MethodDescriptor method;
    private final RpcClientController controller;
    private final Message param;
    private final Message responsePrototype;
    private final RpcCallback<Message> done;

    /**
     * Construct a new method invocation.
     *
     * @param method The method being invoced.
     * @param controller The controller being used for the invocation.
     * @param param The parameter the method will be invoked with.
     * @param responsePrototype A prototype for the response value.
     * @param done The method to call when the invocation returns.
     */
    public RpcClientSideInvocation(
            final MethodDescriptor method,
            final RpcController controller,
            final Message param,
            final Message responsePrototype,
            final RpcCallback<Message> done) {

        this.method = checkNotNull(method);

        final RpcClientControllerImpl rcci =
                (RpcClientControllerImpl) controller;
        this.controller = checkNotNull(rcci);
        rcci.bindToInvocation(this);

        this.param = checkNotNull(param);
        this.responsePrototype = checkNotNull(responsePrototype);
        this.done = checkNotNull(done);
    }

    /**
     * @return The method being invoked.
     */
    public MethodDescriptor getMethod() {
        return method;
    }

    /**
     * @return  The controller.
     */
    public RpcClientController getController() {
        return controller;
    }

    /**
     * @return  the request
     */
    public Message getRequest() {
        return param;
    }

    /**
     * @return A prototype for the return value.
     */
    public Message getResponsePrototype() {
        return responsePrototype;
    }

    /**
     * @return The callback to return the result from the invocation with.
     */
    public RpcCallback<Message> getDone() {
        return done;
    }
}
