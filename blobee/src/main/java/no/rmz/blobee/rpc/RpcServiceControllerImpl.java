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

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import no.rmz.blobeeproto.api.proto.Rpc.MessageType;
import no.rmz.blobeeproto.api.proto.Rpc.RpcControl;

public final class RpcServiceControllerImpl implements RpcController {

    private final RemoteExecutionContext executionContext;

    private final Object monitor = new Object();


    RpcServiceControllerImpl(final RemoteExecutionContext dc) {
        this.executionContext = checkNotNull(dc);
    }

    public void reset() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    boolean failed = false;
    public boolean failed() {
        return failed;
    }

    public String errorText() {
       throw new UnsupportedOperationException("Not supported in server side RpcController");
    }


    private RpcCallback<Object> callbackOnFailure;
    public void notifyOnCancel(final RpcCallback<Object> callback) {
        checkNotNull(callback);
        synchronized (monitor) {
            if (callbackOnFailure != null) {
                throw new IllegalStateException("notifyOnCancel invoked more than once");
            }
            callbackOnFailure = callback;
        }
    }

    private boolean startCancelInvokedAlready = false;
    private boolean cancelled = false;


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

    public void startCancel() {
        synchronized (monitor) {
            cancelled = true;
            invokeCancelledCallback();
        }
    }

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

    public boolean isCanceled() {
       synchronized (monitor) {
           return cancelled;
       }
    }
}
