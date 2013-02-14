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

import static com.google.common.base.Preconditions.checkNotNull;
import java.util.Map;
import java.util.WeakHashMap;
import no.rmz.blobee.rpc.RpcSetup;
import org.jboss.netty.channel.Channel;

public final class MultiChannelClientFactory implements RpcClientFactory {

    private final Object monitor = new Object();

    private final MethodSignatureResolver resolver;
    private Map<Channel, RpcClient> channelClientMap;

    public MultiChannelClientFactory() {
        this.resolver = new ResolverImpl();
        this.channelClientMap = new WeakHashMap<>();
    }

    @Override
    public RpcClient getClientFor(final Channel channel) {
        checkNotNull(channel);
        synchronized (monitor) {
            if (channelClientMap.containsKey(channel)) {
                return channelClientMap.get(channel);
            } else {
                final RpcClient result =
                        new RpcClientImpl(
                            RpcSetup.DEFAULT_BUFFER_SIZE,
                            resolver);
                channelClientMap.put(channel, result);
                return result;
            }
        }
    }

    @Override
    public void removeClientFor(final Channel channel) {
        checkNotNull(channel);
        synchronized (monitor) {
            if (channelClientMap.containsKey(channel)) {
                channelClientMap.remove(channel);
            }
        }
    }

    @Override
    public MethodSignatureResolver getResolver() {
        return resolver;
    }
}
