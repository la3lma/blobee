package no.rmz.blobee.rpc;

import static com.google.common.base.Preconditions.checkNotNull;
import no.rmz.blobeeproto.api.proto.Rpc;
import org.jboss.netty.channel.Channel;

/**
 * A stub for a heartbeat monitor that will detect when the
 * the thing in the other end seems to be hard to get hold of.
 */
public final class HeartbeatMonitor {
    /**
     * A constant used when sending heartbeats.
     */
    private static final Rpc.RpcControl HEARTBEAT =
            Rpc.RpcControl.newBuilder().setMessageType(Rpc.MessageType.HEARTBEAT).build();
    final Channel channel;

    public HeartbeatMonitor(final Channel channel) {
        this.channel = checkNotNull(channel);
        sendHeartbeat();
    }

    public void sendHeartbeat() {
        WireFactory.getWireForChannel(channel).write(HEARTBEAT);
    }

    public void receiveHeartbeat() {
        // XXX Right now, just ignore it.
    }
}
