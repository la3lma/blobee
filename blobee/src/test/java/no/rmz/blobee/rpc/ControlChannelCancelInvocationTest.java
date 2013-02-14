/**
 * Copyright 2013 Bjørn Remseth (la3lma@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
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
// XXX This test is way way way to complex.
@RunWith(MockitoJUnitRunner.class)
public final class ControlChannelCancelInvocationTest {

    private static final Logger log = Logger.getLogger(
            no.rmz.blobee.rpc.ControlChannelCancelInvocationTest.class.getName());
    private static  final String HOST = "localhost";
    public  static  final String RETURN_VALUE = "Going home";
    private RpcChannel clientChannel;
    private Testservice.RpcParam request =
            Testservice.RpcParam.newBuilder().build();
    private RpcController clientController;
    private static  final String FAILED_TEXT = "The computation failed";
    private ClientServerFixture csf;
    private volatile boolean cancelMessageWasReceived;

    private void startClientAndServer(final RpcMessageListener ml) {
        csf = new ClientServerFixture(new ServiceTestItem(), ml);
    }

    @After
    public void shutDown() {
        csf.stop();
    }

    private volatile boolean zot = false;

    /**
     * The service instance that we will use to communicate over the controller
     * channel.
     */
    public final class ServiceTestItem extends Testservice.RpcService {

        private final Testservice.RpcResult result =
                Testservice.RpcResult.newBuilder()
                .setReturnvalue(RETURN_VALUE).build();

        @Override
        public void invoke(
                final RpcController controller,
                final Testservice.RpcParam request,
                final RpcCallback<Testservice.RpcResult> done) {


            controller.notifyOnCancel(new RpcCallback<Object>() {
                @Override
                public void run(final Object parameter) {
                    if (controller.isCanceled()) {
                        cancelMessageWasReceived = true;
                    }
                    Conditions.signalCondition("service:cancellationReceived",
                            cancelLock, cancellationReceived);
                }
            });

            Conditions.signalCondition("service:remoteInvokeStarted",
                    cancelLock, remoteInvokeStarted);
            Conditions.waitForCondition("service:cancellationSent",
                    cancelLock, cancellationSent);

            // XXX Shouldn't happen!  Will not get through since the
            //     the invocation is failed at this point.  Should just be
            //     dropped.
            //  controller.setFailed(FAILED_TEXT);
            done.run(result);
        }
    }
    private final RpcMessageListener rpcMessageListener =
            new RpcMessageListener() {
                @Override
                public void receiveMessage(
                        final Object message,
                        final ChannelHandlerContext ctx) {
                    log.log(Level.INFO, "message = {0}", message);
                }
            };
    private Lock resultLock;
    private Lock cancelLock;
    private Condition resultReceivedCondition;
    private Condition cancellationReceived;
    private Condition remoteInvokeStarted;
    private Condition cancellationSent;
    private RpcController servingController;

    @Before
    public void setUp() {

        cancelMessageWasReceived = false;

        resultLock = new ReentrantLock();
        resultReceivedCondition = resultLock.newCondition();

        cancelLock = new ReentrantLock();
        cancellationReceived = cancelLock.newCondition();
        cancellationSent = cancelLock.newCondition();
        remoteInvokeStarted = cancelLock.newCondition();

        startClientAndServer(rpcMessageListener);

        clientChannel = csf.getClient().newClientRpcChannel();
        clientController = csf.getClient().newController();
    }
    @Mock
    Receiver<String> callbackResponse;

    @Test // (timeout=10000)
    @SuppressWarnings("WA_AWAIT_NOT_IN_LOOP")
    public void testRpcInvocation() throws InterruptedException {

        final RpcCallback<Testservice.RpcResult> callback =
                new RpcCallback<Testservice.RpcResult>() {
                    @Override
                    public void run(final Testservice.RpcResult response) {
                        callbackResponse.receive(response.getReturnvalue());
                        Conditions.signalCondition(
                                "resultReceivedCondition",
                                resultLock,
                                resultReceivedCondition);
                    }
                };

        final Testservice.RpcService myService =
                Testservice.RpcService.newStub(clientChannel);

        final Runnable testRun = new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.currentThread().sleep(1000);
                } catch (InterruptedException ex) {
                    throw new RuntimeException("Interrupted while sleeping");
                }
                myService.invoke(clientController, request, callback);
            }
        };

        new Thread(testRun).start();

        // Signal that cancel is sent, then wait for
        // the things to propagate to all the places they need to propagate
        // to.
        Conditions.waitForCondition("main:remoteInvokeStarted",
                cancelLock, remoteInvokeStarted);
        clientController.startCancel();
        Conditions.signalCondition("main:cancellationSent",
                cancelLock, cancellationSent);
        Conditions.waitForCondition("main:cancellationReceived",
                cancelLock, cancellationReceived);

        // Pass the test if we didn't get a callback response.
        verifyZeroInteractions(callbackResponse);
        assertTrue(cancelMessageWasReceived);
    }
}
