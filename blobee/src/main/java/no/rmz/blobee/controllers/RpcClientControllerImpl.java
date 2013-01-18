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

package no.rmz.blobee.controllers;

import no.rmz.blobee.controllers.RpcClientController;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import no.rmz.blobee.rpc.RpcClient;
import no.rmz.blobee.rpc.RpcClientSideInvocation;

public final class RpcClientControllerImpl implements RpcClientController {

    private boolean failed = false;
    private boolean cancelled = false;
    private String reason = "";
    private final Object monitor = new Object();
    private RpcClient rpcClient;
    private long rpcIndex = -1;
    private boolean active = false;

    public RpcClientControllerImpl() {
    }

    public void reset() {
        synchronized (monitor) {
            if (rpcClient == null) {
                return;
            }
            else if (active) {
                throw new IllegalStateException(
                        "Cannot reset controller, it is already connected to "
                       + " rpcClient = " + rpcClient
                       + " rpcIndex = " + rpcIndex);
            } else {
                rpcClient = null;
                rpcIndex = -1;
            }
        }
    }

    @Override
    public boolean isActive() {
        synchronized (monitor) {
            return active;
        }
    }

    @Override
    public void setActive(final boolean active) {
        synchronized (monitor) {
            this.active = active;
        }
    }


    public boolean failed() {
        synchronized (monitor) {
            return failed;
        }
    }

    public String errorText() {
        synchronized (reason) {
            return reason;
        }
    }

    public void startCancel() {
        synchronized (monitor) {
            cancelled = true;
            if (rpcClient != null) {
                rpcClient.cancelInvocation(rpcIndex);
            }
        }
    }

    public void setFailed(final String reason) {
        checkNotNull(reason);
        synchronized (monitor) {
            failed = true;
            this.reason = reason;
        }
    }

    public boolean isCanceled() {
        synchronized (monitor) {
            return cancelled;
        }
    }

    public void notifyOnCancel(RpcCallback<Object> callback) {
        throw new UnsupportedOperationException("notifyOnCancel callback not supported on client side");
    }

    private RpcClientSideInvocation invocation;

    public void bindToInvocation(final RpcClientSideInvocation invocation) {
        checkNotNull(invocation);
        synchronized (monitor) {
            if (this.invocation != null) {
                throw new IllegalStateException("invocation was non null");
            }
            this.invocation = invocation;
        }
    }


    @Override
    public void setClientAndIndex(final RpcClient rpcClient, final long rpcIndex) {
        checkNotNull(rpcClient);
        checkArgument(rpcIndex >= 0);
        synchronized (monitor) {

            if (this.rpcClient != null) {
                throw new IllegalArgumentException("Controller is already in use, can't be reset");
            }
            this.rpcClient = rpcClient;
            this.rpcIndex  = rpcIndex;
        }
    }

    @Override
    public long getIndex() {
       return rpcIndex;
    }
}
