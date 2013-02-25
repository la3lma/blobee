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
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import no.rmz.blobee.rpc.client.BlobeeRpcController;
import no.rmz.blobee.rpc.server.IllegalReturnException;
import no.rmz.blobeetestproto.api.proto.Testservice;
import no.rmz.blobeetestproto.api.proto.Testservice.RpcResult;
import no.rmz.testtools.Conditions;
import no.rmz.testtools.Receiver;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import static org.mockito.Mockito.*;

/**
 * Test functionality to let a method be invoked, but assume that it will never
 * ever return a value.
 */
@RunWith(MockitoJUnitRunner.class)
// @Ignore // XXX This functionality is being developed right now, test broken
public final class NotReturningInvocationTest {

    private static final Logger log = Logger.getLogger(
            no.rmz.blobee.rpc.NotReturningInvocationTest.class.getName());
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
    private Condition invocationReceived;
    private Condition returningFailed;
    @Mock
    private Receiver<String> callbackResponse;

    @After
    public void shutDown() {
        csf.stop();
    }

    /**
     * The service instance that we will use to communicate over the controller
     * channel.
     */
    public abstract class AbstractTestItem extends Testservice.RpcService {

        public static final String RETURN_VALUE = "Going home";
        private final Testservice.RpcResult result =
                Testservice.RpcResult
                .newBuilder().setReturnvalue(RETURN_VALUE).build();

        /**
         * Subclasses override this to get the appropriate "return" behavior.
         *
         * @param done The callback method to call to send stuff back.
         */
        abstract void returnValue(final RpcCallback<Testservice.RpcResult> done);

        @Override
        public final void invoke(
                final RpcController controller,
                final Testservice.RpcParam request,
                final RpcCallback<Testservice.RpcResult> done) {

            org.junit.Assert.assertTrue(( (BlobeeRpcController) controller ).isNoReturn());
            Conditions.signalCondition("invocationReceived", lock, invocationReceived);
            returnValue(done);
        }

        public final Testservice.RpcResult getResult() {
            return result;
        }
    }

    /**
     * The service instance that we will use to communicate over the controller
     * channel.
     */
    public final class FailingServiceTestItem extends AbstractTestItem {

        @Override
        void returnValue(final RpcCallback<RpcResult> done) {
            //  This should fail, that failure should throw an exception,
            //  and we'll signal the receptionof that exception as a
            //  condition.
            try {
                done.run(getResult());
            }
            catch (IllegalReturnException e) {
                Conditions.signalCondition("returningFailed", lock, returningFailed);
            }
        }
    }

    /**
     * The service instance that we will use to communicate over the controller
     * channel.
     */
    public final class PassingServiceTestItem extends AbstractTestItem {

        @Override
        void returnValue(final RpcCallback<RpcResult> done) {
            // Doing nothing is the right thing to do in this case.
        }
    }
    private RpcCallback<Testservice.RpcResult> callback;

    public void setUp(final Testservice.RpcService service) {

        checkNotNull(service);
        lock = new ReentrantLock();
        resultsReceived = lock.newCondition();
        returningFailed = lock.newCondition();
        invocationReceived = lock.newCondition();

        csf = new ClientServerFixture(new FailingServiceTestItem(), null);

        clientChannel = csf.getClient().newClientRpcChannel();
        clientController = csf.getClient().newController();
        clientController.setNoReturn();

        callback = new RpcCallback<Testservice.RpcResult>() {
            @Override
            public void run(final Testservice.RpcResult response) {
                callbackResponse.receive(response.getReturnvalue());
            }
        };
    }

    public void invokeRemoteProcedure() {
        final Testservice.RpcService myService =
                Testservice.RpcService.newStub(clientChannel);
        myService.invoke(clientController, request, callback);
    }

    @Test// (timeout = 3000)
    @SuppressWarnings("WA_AWAIT_NOT_IN_LOOP")
    public void testFailureByReturningResult() throws InterruptedException {
        setUp(new FailingServiceTestItem());

        invokeRemoteProcedure();
        //  Conditions.waitForCondition("invocationReceived", lock, invocationReceived);
        Conditions.waitForCondition("returningFailed", lock, returningFailed);
        verifyZeroInteractions(callbackResponse);
    }

    @Test(timeout = 3000)
    @SuppressWarnings("WA_AWAIT_NOT_IN_LOOP")
    public void testSuccesseByReturningNoResult() throws InterruptedException {
        setUp(new PassingServiceTestItem());

        invokeRemoteProcedure();

        Conditions.waitForCondition("invocationReceived", lock, invocationReceived);
        verifyZeroInteractions(callbackResponse);
    }
}
