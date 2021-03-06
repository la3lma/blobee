package no.rmz.blobee.rpc;

import java.io.IOException;
import java.net.InetSocketAddress;
import no.rmz.blobee.rpc.client.RpcClient;
import no.rmz.blobee.rpc.peer.RpcMessageListener;
import no.rmz.blobee.rpc.server.RpcServer;
import no.rmz.blobee.rpc.server.RpcServerException;
import no.rmz.blobeetestproto.api.proto.Testservice;
import no.rmz.testtools.Net;

/**
 * Sets up a simple server/client fixture that is  useful in a lot of
 * test scenarios.
 */
public final class ClientServerFixture {
    private static final  String HOST = "localhost";
    private final RpcServer rpcServer;
    private final RpcClient rpcclient;
    private final int port;

    public ClientServerFixture(
            final Testservice.RpcService service,
            final RpcMessageListener ml) {
        try {
            port = Net.getFreePort();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        try {
            this.rpcServer = RpcSetup
                    .newServer(new InetSocketAddress(HOST, port), ml)
                    .addImplementation(
                        service,
                        Testservice.RpcService.Interface.class)
                    .start();
        } catch (RpcServerException ex) {
            throw new RuntimeException(ex);
        }
        this.rpcclient = RpcSetup.newClient(new InetSocketAddress(HOST, port))
                .addInterface(Testservice.RpcService.class)
                .start();
    }

    public void stop() {
        rpcclient.stop();
        rpcServer.stop();
    }

    public RpcClient getClient() {
        return rpcclient;
    }
}
