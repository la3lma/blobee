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

import no.rmz.blobee.rpc.methods.MethodSignatureResolver;
import static com.google.common.base.Preconditions.checkNotNull;
import org.jboss.netty.channel.Channel;

public final class SingeltonClientFactory implements RpcClientFactory {
    private final Object monitor = new Object();
    private final RpcClient rpcClient;
    private Channel channel;

    public SingeltonClientFactory(final RpcClient rpcClient) {
        this.rpcClient = checkNotNull(rpcClient);
    }

    @Override
    public RpcClient getClientFor(final Channel channel) {
        synchronized (monitor) {
            if (this.channel == null) {
                this.channel = channel;
            }
            if (channel != this.channel) {
                throw new IllegalStateException(
                        "Attempt to get client for more than one channel "
                        + channel);
            } else {
                return rpcClient;
            }
        }
    }

    @Override
    public MethodSignatureResolver getResolver() {
        return rpcClient.getResolver();
    }

    @Override
    public void removeClientFor(final Channel channel) {
        synchronized (monitor) {
            checkNotNull(channel);
            if (channel == this.channel) {
                this.channel = null;
            }
        }
    }
}
