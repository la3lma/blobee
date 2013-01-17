package no.rmz.blobee.rpc;

import no.rmz.blobeeproto.api.proto.Rpc;
import org.jboss.netty.channel.ChannelHandlerContext;


public interface  RpcExecutionService {

    public void execute(RemoteExecutionContext dc, ChannelHandlerContext ctx, Object message);

    public Class getReturnType(final Rpc.MethodSignature sig);

     public Class getParameterType(final Rpc.MethodSignature sig);

}
