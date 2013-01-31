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

import no.rmz.blobee.rpc.peer.RpcMessageListener;
import no.rmz.blobee.rpc.server.ExecutionServiceException;
import no.rmz.blobee.rpc.server.RpcExecutionService;
import no.rmz.blobee.rpc.server.RpcExecutionServiceImpl;
import no.rmz.blobee.rpc.client.RpcClient;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcChannel;
import com.google.protobuf.RpcController;
import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import no.rmz.blobee.serviceimpls.SampleServerImpl;
import no.rmz.blobeeprototest.api.proto.Testservice;
import no.rmz.testtools.Net;
import no.rmz.testtools.Receiver;
import org.jboss.netty.channel.ChannelHandlerContext;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * In this class we test the functionality of the control channel by sending
 * various kinds of messages over it, such as error messages, instructions to
 * halt execution of an ongoing computation etc.
 */
@RunWith(MockitoJUnitRunner.class)
public final class ControlChannelFailedInvocationTest {

    private static final Logger log = Logger.getLogger(
            no.rmz.blobee.rpc.ControlChannelFailedInvocationTest.class.getName());
    private final static String HOST = "localhost";
    private int port;
    private RpcChannel clientChannel;
    private Testservice.RpcParam request = Testservice.RpcParam.newBuilder().build();
    private RpcController clientController;

     private final static String FAILED_TEXT = "The computation failed";


    /**
     * The service instance that we will use to communicate over the controller
     * channel.
     */
    public final class ServiceTestItem extends Testservice.RpcService {

        public final static String RETURN_VALUE = "Going home";
        private final Testservice.RpcResult result =
                Testservice.RpcResult.newBuilder().setReturnvalue(RETURN_VALUE).build();


        @Override
        public void invoke(
                final RpcController controller,
                final Testservice.RpcParam request,
                final RpcCallback<Testservice.RpcResult> done) {
            controller.setFailed(FAILED_TEXT);
            done.run(result);
        }
    }
    RpcMessageListener rpcMessageListener = new RpcMessageListener() {
        public void receiveMessage(
                final Object message,
                final ChannelHandlerContext ctx) {
            log.log(Level.INFO, "message = {0}", message);
        }
    };
    private Lock lock;
    private Condition resultReceived;
    private RpcController servingController;

    private void signalResultReceived() {
        try {
            lock.lock();
            resultReceived.signal();
        }
        finally {
            lock.unlock();
        }
    }

    @Before
    public void setUp() throws
            NoSuchMethodException,
            IllegalAccessException,
            IllegalArgumentException,
            InvocationTargetException,
            IOException,
            SecurityException,
            IllegalStateException,
            ExecutionServiceException {

        lock = new ReentrantLock();
        resultReceived = lock.newCondition();
        port = Net.getFreePort();

        final RpcExecutionService executionService;
        executionService = new RpcExecutionServiceImpl(
                "Test service for class " + this.getClass().getName(),
                new ServiceTestItem(),
                Testservice.RpcService.Interface.class);

        final RpcClient client = RpcSetup.newClient(new InetSocketAddress(HOST, port));
        client.addProtobuferRpcInterface(Testservice.RpcService.newReflectiveService(null));

        // XXX This is an abomination,  What is really needed is a
        //     "server client" implementation that the -server- can use
        //     when it has to behave as a client, either to call some other
        //     service or to receive responses. This server-client should
        //     simply reuse the connection of the server, and not have its
        //     own connection to a remote server.  That should be it, but
        //     it's not there yet :-/

        // XXXX this is completely bogus
        RpcSetup.newServer(port, executionService,  rpcMessageListener);

        client.start();

        clientChannel = client.newClientRpcChannel();
        clientController = client.newController();
    }
    @Mock
    Receiver<String> callbackResponse;

    @Test(timeout=10000)
    @SuppressWarnings("WA_AWAIT_NOT_IN_LOOP")
    public void testRpcInvocation() throws InterruptedException {

        final RpcCallback<Testservice.RpcResult> callback =
                new RpcCallback<Testservice.RpcResult>() {
                    public void run(final Testservice.RpcResult response) {
                        callbackResponse.receive(response.getReturnvalue());
                        signalResultReceived();
                    }
                };

        final Testservice.RpcService myService = Testservice.RpcService.newStub(clientChannel);
        myService.invoke(clientController, request, callback);

        try {
            lock.lock();
            log.info("Awaiting result received.");
            resultReceived.await();
        }

        finally {
            lock.unlock();
            log.info("unlocked, test passed");
        }

        verify(callbackResponse).receive(SampleServerImpl.RETURN_VALUE);
        assertTrue(clientController.failed());
        assertEquals(FAILED_TEXT, clientController.errorText());
    }
}
