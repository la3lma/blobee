package no.rmz.blobee.rpc;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.protobuf.Message;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Logger;
import org.jboss.netty.channel.Channel;

public final class WireFactory {

    private final static Logger log = Logger.getLogger(WireFactory.class.getName());

    private static final Map<Channel, MessageWire> wireMap = new WeakHashMap<Channel, MessageWire>();

    private WireFactory() {
    }

    public static MessageWire getWireForChannel(final Channel channel) {
        // XXX Another layer of synchronization first perhaps, to get
        //     a lock-object that nobody else can do anything (including locking)
        //     on?
        synchronized (channel) {
            MessageWire wire = wireMap.get(channel);
            if (wire == null) {
                wire = new MessageWireImpl(channel);
                wireMap.put(channel, wire);
            }
            return wire;
        }
    }

    private static class MessageWireImpl implements MessageWire {

        private final Channel channel;

        public MessageWireImpl(Channel channel) {
            this.channel = channel;
        }
        final Object monitor = new Object();

        public void write(final Message msg1, final Message msg2) {
            checkNotNull(msg1);
            checkNotNull(msg2);
            synchronized (monitor) {
                channel.write(msg1);
                channel.write(msg2);

                // XXX Remove this asap
                log.info(String.format(
                        "Just sent a two-part message over the wire #1='%s', #2='%s'.",
                        msg1, msg2));
            }
        }

        public void write(final Message msg1) {
            checkNotNull(msg1);
            synchronized (monitor) {
                channel.write(msg1);

                // XXX Remove this asap
                log.info(String.format(
                        "Just sent a one-part message over the wire #1='%s'.",
                        msg1));
            }
        }
    }
}
