package no.rmz.blobee.rpc;

import org.jboss.netty.channel.Channel;

public interface RpcClientFactory {
    RpcClientImpl getClientFor(final Channel channel);
}
