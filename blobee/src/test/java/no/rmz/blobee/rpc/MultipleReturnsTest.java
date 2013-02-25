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

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcChannel;
import com.google.protobuf.RpcController;
import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import no.rmz.blobee.rpc.client.BlobeeRpcController;
import no.rmz.blobee.rpc.peer.RpcMessageListener;
import no.rmz.blobee.rpc.server.IllegalMultiReturnException;
import no.rmz.blobeetestproto.api.proto.Testservice;
import no.rmz.testtools.Conditions;
import no.rmz.testtools.Receiver;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Test functionality to let a method return values more than one time. Can be
 * used to "subscribe" to results from a source.
 */
@RunWith(MockitoJUnitRunner.class)
// XXX This is work in progress and the tests below break,
//     so I'm ignoring while debugging. No commits will break
//     existing tests that are not also work in progress.
// @Ignore
public final class MultipleReturnsTest {

    private static final Logger log = Logger.getLogger(
            no.rmz.blobee.rpc.MultipleReturnsTest.class.getName());
    /**
     * Number of results that should be transmitted by the method.
     */
    private final static int NO_OF_REPETITIONS = 50;
    private RpcChannel clientChannel;
    private Testservice.RpcParam request =
            Testservice.RpcParam.newBuilder().build();
    private BlobeeRpcController clientController;
    private static final String FAILED_TEXT = "The computation failed";
    private ClientServerFixture csf;
    private Lock lock;
    private Condition resultsReceived;
    private Condition resultsSent;
    private Condition doneRunFailed;
    @Mock
    private Receiver<String> callbackResponse;

    private void startClientAndServer(final RpcMessageListener ml) {
        csf = new ClientServerFixture(new ServiceTestItem(), ml);
    }

    private void startClientAndServer() {
        csf = new ClientServerFixture(new ServiceTestItem(), null);
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

        public static final String RETURN_VALUE = "Going home";
        private final Testservice.RpcResult result =
                Testservice.RpcResult
                .newBuilder().setReturnvalue(RETURN_VALUE).build();

        @Override
        public void invoke(
                final RpcController controller,
                final Testservice.RpcParam request,
                final RpcCallback<Testservice.RpcResult> done) {
            checkNotNull(controller);
            checkNotNull(request);
            checkNotNull(done);

            try {
                // Return the result many times
                for (int i = 0; i < NO_OF_REPETITIONS; i++) {
                    done.run(result);
                }
            } catch (IllegalMultiReturnException e) {
                Conditions.signalCondition("done.run Failed.",
                        lock,
                        doneRunFailed);
            } catch (Exception e) {
                fail("Caught an unknown exception: " + e);
            }
            Conditions.signalCondition("resultsSent", lock, resultsSent);
        }
    }

    @Before
    public void setUp() {

        lock = new ReentrantLock();
        resultsReceived = lock.newCondition();
        resultsSent = lock.newCondition();
        doneRunFailed = lock.newCondition();

        startClientAndServer();

        clientChannel = csf.getClient().newClientRpcChannel();
        clientController = csf.getClient().newController();
    }

    @Test(timeout = 3000)
    @SuppressWarnings("WA_AWAIT_NOT_IN_LOOP")
    public void testCorrectMultiReturn() throws InterruptedException {

        clientController.setMultiReturn();
        final CountDownLatch countdownLatch = new CountDownLatch(NO_OF_REPETITIONS);

        final RpcCallback<Testservice.RpcResult> callback =
                new RpcCallback<Testservice.RpcResult>() {
                    @Override
                    public void run(final Testservice.RpcResult response) {
                        callbackResponse.receive(response.getReturnvalue());
                        countdownLatch.countDown();
                    }
                };

        final Testservice.RpcService myService =
                Testservice.RpcService.newStub(clientChannel);
        clientController.isMultiReturn();
        myService.invoke(clientController, request, callback);

        Conditions.waitForCondition("resultsSent", lock, resultsSent);

        countdownLatch.await();
        verify(callbackResponse, times(NO_OF_REPETITIONS))
                .receive(ServiceTestItem.RETURN_VALUE);
    }

    @Test//(timeout = 3000)
    @SuppressWarnings("WA_AWAIT_NOT_IN_LOOP")
    public void testMultiReturnWithoutMultiSetInController() throws InterruptedException {

        final CountDownLatch countdownLatch = new CountDownLatch(NO_OF_REPETITIONS);

        final RpcCallback<Testservice.RpcResult> callback =
                new RpcCallback<Testservice.RpcResult>() {
                    @Override
                    public void run(final Testservice.RpcResult response) {
                        callbackResponse.receive(response.getReturnvalue());
                        countdownLatch.countDown();
                    }
                };

        final Testservice.RpcService myService =
                Testservice.RpcService.newStub(clientChannel);

        assertFalse(clientController.isMultiReturn());
        myService.invoke(clientController, request, callback);

        Conditions.waitForCondition("done.run Failed", lock, doneRunFailed);
        
        verify(callbackResponse, times(1))
                .receive(ServiceTestItem.RETURN_VALUE);

        assertEquals(countdownLatch.getCount(), NO_OF_REPETITIONS - 1);
    }
}
