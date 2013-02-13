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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.protobuf.RpcCallback;
import no.rmz.blobee.rpc.client.RpcClientImpl;
import no.rmz.blobee.rpc.client.RpcClientSideInvocation;

public final class RpcClientControllerImpl implements RpcClientController {

    private boolean failed = false;
    private boolean cancelled = false;
    private String reason = "";
    private final Object monitor = new Object();
    private RpcClientImpl rpcClient;
    private long rpcIndex = -1;
    private boolean active = false;
    private RpcClientSideInvocation invocation;

    public RpcClientControllerImpl() {
    }

    public void reset() {
        synchronized (monitor) {
            if (rpcClient == null) {
                return;
            } else if (active) {
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

    @Override
    public boolean failed() {
        synchronized (monitor) {
            return failed;
        }
    }

    @Override
    public String errorText() {
        synchronized (reason) {
            return reason;
        }
    }

    @Override
    public void startCancel() {
        synchronized (monitor) {
            cancelled = true;
            if (rpcClient != null) {
                rpcClient.cancelInvocation(rpcIndex);
            }
        }
    }

    @Override
    public void setFailed(final String reason) {
        checkNotNull(reason);
        synchronized (monitor) {
            failed = true;
            this.reason = reason;
        }
    }

    @Override
    public boolean isCanceled() {
        synchronized (monitor) {
            return cancelled;
        }
    }

    @Override
    public void notifyOnCancel(final RpcCallback<Object> callback) {
        throw new UnsupportedOperationException(
                "notifyOnCancel callback not supported on client side");
    }

    @Override
    public void setClientAndIndex(
            final RpcClientImpl rpcClient,
            final long rpcIndex) {
        checkNotNull(rpcClient);
        checkArgument(rpcIndex >= 0);
        synchronized (monitor) {

            if (this.rpcClient != null) {
                throw new IllegalArgumentException(
                        "Controller is already in use, can't be reset");
            }
            this.rpcClient = checkNotNull(rpcClient);
            checkArgument(rpcIndex >= 0);
            this.rpcIndex = rpcIndex;
        }
    }

    @Override
    public long getIndex() {
        return rpcIndex;
    }
}
