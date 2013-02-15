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
import no.rmz.blobee.rpc.peer.wireprotocol.OutgoingWireAdapter;
import no.rmz.blobee.rpc.peer.wireprotocol.WireFactory;

public final class RpcServiceControllerImpl implements RpcServiceController {

    private final RemoteExecutionContext executionContext;
    private final Object monitor = new Object();
    private boolean failed = false;
    private boolean startCancelInvokedAlready = false;
    private boolean cancelled = false;
    private RpcCallback<Object> callbackOnFailure;
    private OutgoingWireAdapter wire;

    public RpcServiceControllerImpl(final RemoteExecutionContext dc) {
        this.executionContext = checkNotNull(dc);
        this.wire = WireFactory.getWireForChannel(
                executionContext.getCtx().getChannel());
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException(
                "Reset not supported on server side controller");
    }

    @Override
    public boolean failed() {
        return failed;
    }

    @Override
    public String errorText() {
        throw new UnsupportedOperationException(
                "Not supported in server side RpcController");
    }


    @Override
    public void notifyOnCancel(final RpcCallback<Object> callback) {
        checkNotNull(callback);
        synchronized (monitor) {
            if (callbackOnFailure != null) {
                throw new IllegalStateException(
                        "notifyOnCancel invoked more than once");
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
        final long rpcIndex = executionContext.getRpcIndex();
        wire.sendInvocationFailedMessage(rpcIndex, reason);
    }

    @Override
    public boolean isCanceled() {
        synchronized (monitor) {
            return cancelled;
        }
    }
}
