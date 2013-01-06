
package no.rmz.blobee.rpc;

import no.rmz.blobee.rpc.RpcPeerHandler.DecodingContext;
import org.jboss.netty.channel.ChannelHandlerContext;


public interface  RpcExecutionService {

    public void execute(DecodingContext dc, ChannelHandlerContext ctx, Object message);

}
