/**
 * Copyright 2013  Bjørn Borud
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
package no.rmz.testtools;

import static com.google.common.base.Preconditions.checkArgument;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

/**
 * Testing utilities for networking.
 *
 * @author borud
 */
public final class Net {

    private  Net() {
    }

    /**
     * Find a network port that is not in use. <p> The only way to implement
     * this sensibly without obvious race conditions would be if we could return
     * an opened listen socket. This way we would only run into trouble if we
     * were unable to find a port we can bind at all. <p> However, since most of
     * the code we need to test doesn't let us inject listen sockets this is
     * impractical. We could of course pass a socket back and have the client
     * code close it and then re-use it, but that would be burdening the
     * developer unduly. <p> This means that the goal of this code is to, with
     * some probability, locate a port number that appears to be free and hope
     * that the time window is narrow enough so other threads or processes
     * cannot grab it before we make use of it.
     *
     * @throws IOException if an IO error occurs
     * @return a port number which is probably free so we can bind it
     */
    public static int getFreePort() throws IOException {
        final int[] port = getFreePorts(1);
        return port[0];
    }

    /**
     * Get multiple ports. This is useful when you need more than one port
     * number before you start binding any of the ports.
     *
     * @param numPorts the number of port numbers we need.
     * @return an array of numPorts port numbers.
     * @throws IOException if an IO error occurs.
     */
    public static int[] getFreePorts(final int numPorts) throws IOException {

        checkArgument(numPorts > 0);
        final List<ServerSocket> sockets =
                new ArrayList<>(numPorts);
        final int[] portNums = new int[numPorts];

        try {
            for (int i = 0; i < numPorts; i++) {
                // Calling the constructor of ServerSocket with the port
                // number argument set to zero has defined semantics: it
                // allocates a free port.
                final ServerSocket ss = new ServerSocket(0);
                ss.setReuseAddress(true);
                sockets.add(ss);
                portNums[i] = ss.getLocalPort();
            }
            return portNums;
        } finally {
            for (final ServerSocket socket : sockets) {
                socket.close();
            }
        }
    }
}
