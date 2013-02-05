package no.rmz.blobee.rpc;

import java.io.IOException;
import java.net.InetSocketAddress;
import no.rmz.blobee.rpc.client.RpcClient;
import no.rmz.blobee.rpc.peer.RpcMessageListener;
import no.rmz.blobee.rpc.server.RpcServer;
import no.rmz.blobee.serviceimpls.SampleServerImpl;
import no.rmz.blobeetestproto.api.proto.Testservice;
import no.rmz.testtools.Net;

public final class ClientServerFixture {
    private final static String HOST = "localhost";
    private final RpcServer rpcServer;
    private final RpcClient rpcclient;
    private final int port;

    public ClientServerFixture(final RpcMessageListener ml) {
        try {
            port = Net.getFreePort();
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        this.rpcServer = RpcSetup
                .newServer(new InetSocketAddress(HOST, port), ml)
                .addImplementation(
                    new SampleServerImpl(),
                    Testservice.RpcService.Interface.class)
                .start();
        this.rpcclient = RpcSetup.newClient(new InetSocketAddress(HOST, port))
                .addInterface(Testservice.RpcService.class)
                .start();
    }

    public void stop() {
        rpcclient.stop();
        rpcServer.stop();
    }
}
