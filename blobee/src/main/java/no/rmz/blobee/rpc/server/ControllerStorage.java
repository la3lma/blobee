package no.rmz.blobee.rpc.server;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import no.rmz.blobee.controllers.RpcServiceController;
import org.jboss.netty.channel.ChannelHandlerContext;
// XXX Use "table" from guava instead?

public final class ControllerStorage {
    private final Map<RpcExecutionServiceImpl.ControllerCoordinate, RpcServiceController> map = new ConcurrentHashMap<RpcExecutionServiceImpl.ControllerCoordinate, RpcServiceController>();

    public void storeController(final ChannelHandlerContext ctx, final long rpcIdx, final RpcServiceController controller) {
        checkNotNull(ctx);
        checkArgument(rpcIdx >= 0);
        checkNotNull(controller);
        map.put(new RpcExecutionServiceImpl.ControllerCoordinate(ctx, rpcIdx), controller);
    }

    public RpcServiceController removeController(final ChannelHandlerContext ctx, final long rpcIdx) {
        return map.remove(new RpcExecutionServiceImpl.ControllerCoordinate(ctx, rpcIdx));
    }

    public RpcServiceController getController(final ChannelHandlerContext ctx, final long rpcIdx) {
        final RpcServiceController result = map.get(new RpcExecutionServiceImpl.ControllerCoordinate(ctx, rpcIdx));
        return result;
    }

}
