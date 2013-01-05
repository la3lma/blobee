package no.rmz.blobee.rpc;

import org.jboss.netty.channel.ChannelHandlerContext;

public interface RpcMessageListener {

    public void receiveMessage(Object message, ChannelHandlerContext ctx);

}
