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
package no.rmz.blobee.rpc.peer;

import static com.google.common.base.Preconditions.checkNotNull;
import no.rmz.blobee.rpc.peer.wireprotocol.OutgoingRpcWire;
import no.rmz.blobee.rpc.peer.wireprotocol.WireFactory;
import org.jboss.netty.channel.Channel;

/**
 * A stub for a heartbeat monitor that will detect when the
 * the thing in the other end seems to be hard to get hold of.
 */
public final class HeartbeatMonitor {

    private final OutgoingRpcWire wire;

    public HeartbeatMonitor(final Channel channel) {
        checkNotNull(channel);
        this.wire =  WireFactory.getWireForChannel(channel);
        sendHeartbeat();
    }

    public void sendHeartbeat() {
        wire.sendHeartbeat();
    }

    public void receiveHeartbeat() {
        // XXX Right now, just ignore it.
    }
}
