/**
 * Copyright 2013  Bjørn Remseth (la3lma@gmail.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package no.rmz.blobee.rpc.peer.wireprotocol;

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

        final Object monitor = new Object();

        private final Channel channel;

        public MessageWireImpl(Channel channel) {
            this.channel = channel;
        }


        public void write(final Message msg1, final Message msg2) {
            checkNotNull(msg1);
            checkNotNull(msg2);
            synchronized (monitor) {
                channel.write(msg1);
                channel.write(msg2);
            }
        }

        public void write(final Message msg1) {
            checkNotNull(msg1);
            synchronized (monitor) {
                channel.write(msg1);
            }
        }
    }
}
