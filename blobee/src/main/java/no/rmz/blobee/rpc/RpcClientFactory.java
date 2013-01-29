package no.rmz.blobee.rpc;

import org.jboss.netty.channel.Channel;

public interface RpcClientFactory {
    RpcClient getClientFor(final Channel channel);
    public void removeClientFor(final Channel channel);
    MethodSignatureResolver getResolver();
}
