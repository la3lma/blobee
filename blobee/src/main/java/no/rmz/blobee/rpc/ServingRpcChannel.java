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

import no.rmz.blobee.controllers.RpcClientControllerImpl;
import com.google.common.base.Function;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.protobuf.BlockingRpcChannel;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcChannel;
import com.google.protobuf.RpcController;
import com.google.protobuf.ServiceException;
import no.rmz.blobeeproto.api.proto.Rpc.MethodSignature;

/**
 * Experimental implementation of an RpcChannel. Used as a vehicle to wrap my
 * mind about the various details involved in making an RPC library.
 */
public final class ServingRpcChannel implements RpcChannel {
    final MethodMap methodMap;

    public ServingRpcChannel(final MethodMap methodMap) {
        this.methodMap = checkNotNull(methodMap);
    }

    @Override
    public void callMethod(
            final MethodDescriptor method,
            final RpcController controller,
            final Message request,
            final Message responsePrototype,
            final RpcCallback<Message> callback) {
        checkNotNull(method);
        checkNotNull(controller);
        checkNotNull(request);
        checkNotNull(responsePrototype);
        checkNotNull(callback);

        // XXX Happy day scenario. Must handle a lot more
        //     bogusness before it's believable :)
        final Function<Message, Message> meth;
        meth = methodMap.getByMethodDescriptor(method);
        final Message result = meth.apply(request);
        callback.run(result);
    }

    public RpcController newController() {
        return new RpcClientControllerImpl();
    }

    // XXX This is a rather bogus api, refactor asap!!
    public BlockingRpcChannel newBlockingRchannel() {
        return new BlockingRpcChannel() {
            public Message callBlockingMethod(
                    final MethodDescriptor method,
                    final RpcController controller,
                    final Message request,
                    final Message responsePrototype) throws ServiceException {

                final Function<Message, Message> meth;
                meth = methodMap.getByMethodDescriptor(method);
                final Message result = meth.apply(request);

                return result;
            }
        };
    }
}
