package no.rmz.blobee.rpc;

import org.jboss.netty.channel.ChannelHandlerContext;


public interface  RpcExecutionService {

    public void execute(RemoteExecutionContext dc, ChannelHandlerContext ctx, Object message);

}
