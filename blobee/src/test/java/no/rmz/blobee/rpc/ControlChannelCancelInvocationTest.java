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
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import no.rmz.blobee.rpc.client.RpcClient;
import no.rmz.blobee.rpc.peer.RpcMessageListener;
import no.rmz.blobee.rpc.server.ExecutionServiceException;
import no.rmz.blobee.rpc.server.RpcExecutionService;
import no.rmz.blobee.rpc.server.RpcExecutionServiceImpl;
import no.rmz.blobeetestproto.api.proto.Testservice;
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
public final class ControlChannelCancelInvocationTest {

    private static final Logger log = Logger.getLogger(
            no.rmz.blobee.rpc.ControlChannelCancelInvocationTest.class.getName());
    private final static String HOST = "localhost";
    public final static String RETURN_VALUE = "Going home";
    private int port;
    private RpcChannel clientChannel;
    private Testservice.RpcParam request = Testservice.RpcParam.newBuilder().build();
    private RpcController clientController;
    private final static String FAILED_TEXT = "The computation failed";


    private static void waitForCondition(
            final String description,
            final Lock lock,
            final Condition condition) {
        try {
            lock.lock();
            log.log(Level.INFO, "Awaiting condition {0}", description);
            condition.await();
            log.log(Level.INFO, "Just finished waiting for condition {0}", description);
        }
        catch (InterruptedException ex) {
            fail("Interrupted: " + ex);
        }
        finally {
            lock.unlock();
        }
    }

    private static void signalCondition(final String description, final Lock lock, final Condition condition) {
        try {
            lock.lock();
            log.log(Level.INFO, "Signalling condition {0}", description);
            condition.signal();
        }
        finally {
            lock.unlock();
        }
    }

    /**
     * The service instance that we will use to communicate over the controller
     * channel.
     */
    public final class ServiceTestItem extends Testservice.RpcService {


        private final Testservice.RpcResult result =
                Testservice.RpcResult.newBuilder().setReturnvalue(RETURN_VALUE).build();

        @Override
        public void invoke(
                final RpcController controller,
                final Testservice.RpcParam request,
                final RpcCallback<Testservice.RpcResult> done) {


            controller.notifyOnCancel(new RpcCallback<Object>() {
                public void run(Object parameter) {
                    if (controller.isCanceled()) {
                        cancelMessageWasReceived.setValue(true);
                    }
                    signalCondition("service:cancellationReceived", cancelLock, cancellationReceived);
                }
            });

            signalCondition("service:remoteInvokeStarted", cancelLock, remoteInvokeStarted);
            waitForCondition("service:cancellationSent", cancelLock, cancellationSent);

            // XXX Shouldn't happen!  Will not get through since the
            //     the invocation is failed at this point.  Should just be
            //     dropped.
            //  controller.setFailed(FAILED_TEXT);
            done.run(result);
        }
    }
    private BooleanHolder cancelMessageWasReceived;

    public final static class BooleanHolder {

        private boolean value;

        public void setValue(final boolean value) {
            this.value = value;
        }

        public boolean getValue() {
            return value;
        }
    }


    private final RpcMessageListener rpcMessageListener =
            new RpcMessageListener() {
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
    public void setUp() throws
            NoSuchMethodException,
            IllegalAccessException,
            IllegalArgumentException,
            InvocationTargetException,
            IOException,
            SecurityException,
            IllegalStateException,
            ExecutionServiceException {

        cancelMessageWasReceived = new BooleanHolder();
        cancelMessageWasReceived.setValue(false);

        // XXX Setting up the locks and conditions
        resultLock = new ReentrantLock();
        resultReceivedCondition = resultLock.newCondition();

        cancelLock = new ReentrantLock();
        cancellationReceived = cancelLock.newCondition();
        cancellationSent = cancelLock.newCondition();
        remoteInvokeStarted = cancelLock.newCondition();

        port = Net.getFreePort();

        final RpcExecutionService executionService;
        executionService = new RpcExecutionServiceImpl(
                "Test service in class " + this.getClass().getName(),
                new ServiceTestItem(),
                Testservice.RpcService.Interface.class);

        final RpcClient client = RpcSetup.newClient(new InetSocketAddress(HOST, port));
        client.addProtobuferRpcInterface(Testservice.RpcService.newReflectiveService(null));

        RpcSetup.deprecatedNewServer(port, executionService, rpcMessageListener);

        client.start();

        clientChannel = client.newClientRpcChannel();
        clientController = client.newController();
    }

    @Mock
    Receiver<String> callbackResponse;

    @Test // (timeout=10000)
    @SuppressWarnings("WA_AWAIT_NOT_IN_LOOP")
    public void testRpcInvocation() throws InterruptedException {

        final RpcCallback<Testservice.RpcResult> callback =
                new RpcCallback<Testservice.RpcResult>() {
                    public void run(final Testservice.RpcResult response) {
                        callbackResponse.receive(response.getReturnvalue());
                        signalCondition(
                                "resultReceivedCondition",
                                resultLock,
                                resultReceivedCondition);
                    }
                };

        final Testservice.RpcService myService =
                Testservice.RpcService.newStub(clientChannel);

        final Runnable testRun = new Runnable() {
            public void run() {
                try {
                    Thread.currentThread().sleep(1000);
                }
                catch (InterruptedException ex) {
                    throw new RuntimeException("Interrupted while sleeping");
                }
                myService.invoke(clientController, request, callback);
            }
        };

        new Thread(testRun).start();


        // Signal that cancel is sent, then wait for
        // the things to propagate to all the places they need to propagate
        // to.
        waitForCondition("main:remoteInvokeStarted", cancelLock, remoteInvokeStarted);
        clientController.startCancel();
        signalCondition("main:cancellationSent", cancelLock, cancellationSent);
        waitForCondition("main:cancellationReceived", cancelLock, cancellationReceived);


        // Pass the test if we didn't get a callback response.
        verifyZeroInteractions(callbackResponse);
        assertTrue(cancelMessageWasReceived.getValue());
    }
}
