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
package no.rmz.blobee.controllers;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.protobuf.RpcCallback;
import no.rmz.blobee.rpc.peer.RemoteExecutionContext;
import no.rmz.blobeeproto.api.proto.Rpc.MessageType;
import no.rmz.blobeeproto.api.proto.Rpc.RpcControl;

public final class RpcServiceControllerImpl implements RpcServiceController {

    private final RemoteExecutionContext executionContext;
    private final Object monitor = new Object();
    boolean failed = false;
    private boolean startCancelInvokedAlready = false;
    private boolean cancelled = false;
    private RpcCallback<Object> callbackOnFailure;

    public RpcServiceControllerImpl(final RemoteExecutionContext dc) {
        this.executionContext = checkNotNull(dc);
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException("Reset not supported on server side controller");
    }

    @Override
    public boolean failed() {
        return failed;
    }

    @Override
    public String errorText() {
        throw new UnsupportedOperationException("Not supported in server side RpcController");
    }


    public void notifyOnCancel(final RpcCallback<Object> callback) {
        checkNotNull(callback);
        synchronized (monitor) {
            if (callbackOnFailure != null) {
                throw new IllegalStateException("notifyOnCancel invoked more than once");
            }
            callbackOnFailure = callback;
        }
    }


    @Override
    public void invokeCancelledCallback() {
        synchronized (monitor) {
            if (!startCancelInvokedAlready) {
                startCancelInvokedAlready = true;
                if (callbackOnFailure != null) {
                    callbackOnFailure.run(null);
                }
            }
        }
    }

    @Override
    public void startCancel() {
        synchronized (monitor) {
            cancelled = true;
            invokeCancelledCallback();
        }
    }

    @Override
    public void setFailed(final String reason) {
        checkNotNull(reason);
        failed = true;
        final RpcControl msg = RpcControl.newBuilder()
                .setMessageType(MessageType.INVOCATION_FAILED)
                .setRpcIndex(executionContext.getRpcIndex())
                .setFailed(reason)
                .build();

        executionContext.sendControlMessage(msg);
    }

    @Override
    public boolean isCanceled() {
        synchronized (monitor) {
            return cancelled;
        }
    }
}
