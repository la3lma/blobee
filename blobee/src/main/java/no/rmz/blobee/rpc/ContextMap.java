package no.rmz.blobee.rpc;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jboss.netty.channel.ChannelHandlerContext;

/**
 * For each ChannelHandlerContext, this map keeps track of the
 * RemoteExecution context being processed. This processing is a two-step
 * process where the RemoteExecutionContext is first established by a
 * control message, and then the next payload message delivers the
 * parameter. This means that we need to store some context between
 * processing of incoming messages, and that is what this map is being used
 * for.
 */
public final class ContextMap {
    private Map<ChannelHandlerContext, RemoteExecutionContext>
            map = new ConcurrentHashMap<ChannelHandlerContext, RemoteExecutionContext>();

    private Map<Long, RemoteExecutionContext>
            mapByIndex = new ConcurrentHashMap<Long, RemoteExecutionContext>();


    public void put(final ChannelHandlerContext chc, final RemoteExecutionContext rec) {
        checkNotNull(chc);
        synchronized (chc) {
            if (rec != null && map.containsKey(chc)) {
                throw new IllegalStateException(
                        "Attempting to set context value befor nulling old one : old={"
                        + map.get(chc) + "}, new ={" + rec + "}.");
            }
            map.put(chc, rec);
            mapByIndex.put(rec.getRpcIndex(), rec);
        }
    }

    public RemoteExecutionContext remove(final ChannelHandlerContext chc) {
        checkNotNull(chc);
        mapByIndex.remove(map.get(chc).getRpcIndex());/// XXX Dangerous
        return map.remove(chc);
    }

    public RemoteExecutionContext get(final ChannelHandlerContext chc) {
        checkNotNull(chc);
        return map.get(chc);
    }

    public RemoteExecutionContext getByIndex(final long rpcIndex) {
        checkArgument(rpcIndex >= 0);
        return mapByIndex.get(rpcIndex);
    }
}
