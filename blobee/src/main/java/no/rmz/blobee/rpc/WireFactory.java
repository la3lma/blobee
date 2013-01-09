package no.rmz.blobee.rpc;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.protobuf.Message;
import java.util.Map;
import java.util.WeakHashMap;
import org.jboss.netty.channel.Channel;

public final class WireFactory {
    private static final Map<Channel, MessageWire> wireMap = new WeakHashMap<Channel, MessageWire>();

    private WireFactory() {
    }


    public static MessageWire getWireForChannel(final Channel channel) {
        synchronized (channel) {
            MessageWire wire = wireMap.get(channel);
            if (wire == null) {
                wire = new MessageWire() {
                    final Object monitor = new Object();

                    public void write(Message msg1, Message msg2) {
                        checkNotNull(msg1);
                        checkNotNull(msg2);
                        synchronized (monitor) {
                            channel.write(msg1);
                            channel.write(msg2);
                        }
                    }

                    public void write(Message msg1) {
                        checkNotNull(msg1);
                        synchronized (monitor) {
                            channel.write(msg1);
                        }
                    }
                };
                wireMap.put(channel, wire);
            }
            return wire;
        }
    }

}
