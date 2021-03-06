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
package no.rmz.blobee.rpc;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcChannel;
import com.google.protobuf.RpcController;
import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import no.rmz.blobee.rpc.client.RpcClient;
import no.rmz.blobee.rpc.peer.RpcMessageListener;
import no.rmz.blobee.rpc.server.RpcServer;
import no.rmz.blobee.rpc.server.RpcServerException;
import no.rmz.blobee.serviceimpls.SampleServerImpl;
import no.rmz.blobeetestproto.api.proto.Testservice;
import no.rmz.testtools.Net;
import no.rmz.testtools.Receiver;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public final class RpcPeerInvocationTest {

    private static final Logger log = Logger.getLogger(
            no.rmz.blobee.rpc.RpcPeerInvocationTest.class.getName());
    private  static  final String HOST = "localhost";

    private int port;

    private RpcChannel clientChannel;
    private Testservice.RpcParam request =
            Testservice.RpcParam.newBuilder().build();
    private RpcController clientController;


    private Lock lock;
    private Condition resultReceived;
    private RpcController servingController;

    private RpcMessageListener rpcMessageListener = new RpcMessageListener() {
        @Override
        public void receiveMessage(
                final Object message,
                final ChannelHandlerContext ctx) {
            log.log(Level.INFO, "message = {0}", message);
        }
    };


    private void signalResultReceived() {
        try {
            lock.lock();
            resultReceived.signal();
        } finally {
            lock.unlock();
        }
    }

    @Before
    @SuppressWarnings("DLS_DEAD_LOCAL_STORE")
    public void setUp()
           throws RpcServerException, IOException {

        lock = new ReentrantLock();
        resultReceived = lock.newCondition();
        port = Net.getFreePort();

        // XXX Use the fixture instead?
        final RpcServer rpcServer =
                RpcSetup.newServer(
                    new InetSocketAddress(HOST, port),
                    rpcMessageListener)
                .addImplementation(
                    new SampleServerImpl(),
                    Testservice.RpcService.Interface.class)
                .start();

        final RpcClient rpcclient =
           RpcSetup
                .newClient(new InetSocketAddress(HOST, port))
                .addInterface(Testservice.RpcService.class)
                .start();

        clientChannel    = rpcclient.newClientRpcChannel();
        clientController = rpcclient.newController();
    }

    @Mock
    Receiver<String> callbackResponse;

    @Test(timeout = 10000)
    @SuppressWarnings("WA_AWAIT_NOT_IN_LOOP")
    public void testRpcInvocation() throws InterruptedException {

        final RpcCallback<Testservice.RpcResult> callback =
                new RpcCallback<Testservice.RpcResult>() {
                    @Override
                    public void run(final Testservice.RpcResult response) {
                        callbackResponse.receive(response.getReturnvalue());
                        signalResultReceived();
                    }
                };

        final Testservice.RpcService myService =
                Testservice.RpcService.newStub(clientChannel);
        myService.invoke(clientController, request, callback);

        try {
            lock.lock();
            log.info("Awaiting result received.");
            resultReceived.await();
        } finally {
            lock.unlock();
            log.info("unlocked, test passed");
        }

        verify(callbackResponse).receive(SampleServerImpl.RETURN_VALUE);
    }
}
