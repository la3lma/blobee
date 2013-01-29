package no.rmz.blobee.rpc;

import static com.google.common.base.Preconditions.checkNotNull;
import org.jboss.netty.channel.Channel;

public final class SingeltonClientFactory implements RpcClientFactory {
    private final Object monitor = new Object();
    private final RpcClient rpcClient;
    private Channel channel;

    public SingeltonClientFactory(final RpcClient rpcClient) {
        this.rpcClient = checkNotNull(rpcClient);
    }

    public RpcClient getClientFor(final Channel channel) {
        synchronized (monitor) {
            if (this.channel == null) {
                this.channel = channel;
            }
            if (channel != this.channel) {
                throw new IllegalStateException("Attempt to get client for more than one channel " + channel);
            } else {
                return rpcClient;
            }
        }
    }

    public MethodSignatureResolver getResolver() {
        return rpcClient.getResolver();
    }

    public void removeClientFor(final Channel channel) {
        synchronized (monitor) {
            checkNotNull(channel);
            if (channel == this.channel) {
                this.channel = null;
            }
        }
    }
}
