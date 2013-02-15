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

import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Logger;
import org.jboss.netty.channel.Channel;

public final class WireFactory {

    private static final  Logger log =
            Logger.getLogger(WireFactory.class.getName());

    private static final Map<Channel, OutgoingWireAdapter> WIRE_MAP =
            new WeakHashMap<>();

    /**
     * Utility class, no public constructor.
     */
    private WireFactory() {
    }

    public static OutgoingWireAdapter getWireForChannel(final Channel channel) {
        // XXX Another layer of synchronization first perhaps, to get
        //     a lock-object that nobody else can do anything
        //     (including locking) on?
        synchronized (channel) {
            OutgoingWireAdapter wire = WIRE_MAP.get(channel);
            if (wire == null) {
                wire = new OutgoingRpcAdapterImpl(channel);
                WIRE_MAP.put(channel, wire);
            }
            return wire;
        }
    }
}
