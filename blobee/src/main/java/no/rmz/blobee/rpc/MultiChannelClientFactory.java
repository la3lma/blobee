package no.rmz.blobee.rpc;

import static com.google.common.base.Preconditions.checkNotNull;
import java.util.Map;
import java.util.WeakHashMap;
import org.jboss.netty.channel.Channel;

public final class MultiChannelClientFactory implements RpcClientFactory {

    private final static int DEFAULT_QUEUE_LENGTH = 1; // XXX SHould be larger.
    private final Object monitor = new Object();
    private Channel channel;
    private final MethodSignatureResolver resolver;
    private Map<Channel, RpcClient> channelClientMap;

    public MultiChannelClientFactory() {
        this.resolver = new ResolverImpl();
        this.channelClientMap = new WeakHashMap<Channel, RpcClient>();
    }

    public RpcClient getClientFor(final Channel channel) {
        checkNotNull(channel);
        synchronized (monitor) {
            if (channelClientMap.containsKey(channel)) {
                return channelClientMap.get(channel);
            } else {
                final RpcClient result =
                        new RpcClientImpl(RpcSetup.DEFAULT_BUFFER_SIZE, resolver);
                channelClientMap.put(channel, result);
                return result;
            }
        }
    }

    public void removeClientFor(final Channel channel) {
        checkNotNull(channel);
        synchronized (monitor) {
            if (channelClientMap.containsKey(channel)) {
                channelClientMap.remove(channel);
            }
        }
    }

    public MethodSignatureResolver getResolver() {
        return resolver;
    }
}
