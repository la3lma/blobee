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
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import no.rmz.blobee.rpc.peer.RpcMessageListener;
import no.rmz.blobeetestproto.api.proto.Testservice;
import no.rmz.testtools.Conditions;
import no.rmz.testtools.Receiver;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.junit.After;
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

    private RpcChannel clientChannel;
    private Testservice.RpcParam request = Testservice.RpcParam.newBuilder().build();
    private RpcController clientController;
    private final static String FAILED_TEXT = "The computation failed";
    private ClientServerFixture csf;

    private void startClientAndServer(final RpcMessageListener ml) {
        csf = new ClientServerFixture(new ServiceTestItem(), ml);
    }

    @After
    public void shutDown() {
        csf.stop();
    }

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
            Conditions.signalCondition("failedSent", lock, failedSent);

            done.run(result);
        }
    }

    private final RpcMessageListener rpcMessageListener = new RpcMessageListener() {
        public void receiveMessage(
                final Object message,
                final ChannelHandlerContext ctx) {
            log.log(Level.INFO, "message = {0}", message);
        }
    };
    private Lock lock;
    private Condition resultReceived;
    private Condition failedSent;


    @Before
    public void setUp() {

        lock = new ReentrantLock();
        resultReceived = lock.newCondition();
        failedSent = lock.newCondition();

        startClientAndServer(rpcMessageListener);

        clientChannel = csf.getClient().newClientRpcChannel();
        clientController = csf.getClient().newController();
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
                         Conditions.waitForCondition("resultReceived", lock, resultReceived);
                    }
                };

        final Testservice.RpcService myService = Testservice.RpcService.newStub(clientChannel);
        myService.invoke(clientController, request, callback);

        Conditions.waitForCondition("failedSent", lock, failedSent);

        verifyZeroInteractions(callbackResponse);

        // XXX This may actually fail due to synchronization issues.
        assertTrue(clientController.failed());
        assertEquals(FAILED_TEXT, clientController.errorText());
    }
}
