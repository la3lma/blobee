How hard can it be to write an RPC framework?
=============================================


Well, the only way to find out is to try I guess, so that's what I'm
doing.  Call it a finger exercise, a design study, whatever. It's not
something that useful for anyone for anything, but do feel free to
take a look and if you find it interesting please tell me. If you send
me a pull request i'll be ecstatic ;-)

Btw, there is no license tagged on to anything here, but when I get 
around to it I'll open source this by tagging an Apache 2 license
to everything.


How to use the RPC library
==========================

The blobee library is now in a pre-alpha state, but it's finished
enough for me to ask for input from other developers.  I know there
are many strange things in the source code, so I'm actually not so
interested in comments on that since I'm in the process of cleaning
it up already.  The  user-facing API however is something I very much
would like to have to get feedback about, so if feel free to give
it  try and please do send me feedback (to la3lma@gmail.com).

Even if you don't write your own servers and clients, please take a look
at the example code below and comment on it.  I really need for
feedback from someone else now :-)

* Clone it from github 

           git clone git://github.com/la3lma/blobee.git

* Compile

           mvn clean install

* Write some code that uses it.   You should  include something like this
   in your pom.xml file.  It will only work if you've compiled the library
   locally, since nothing has been uploaded to maven central yet.

        <dependency>
            <groupId>no.rmz</groupId>
            <artifactId>blobee</artifactId>
            <version>1-0.SNAPSHOT</version>
        </dependency>

*  Write some code.   This is a test program from the  project itself, 
   but it should be able to serve as a template both for writing clients
   and servers.


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
             import java.io.IOException;
             import java.net.InetSocketAddress;
             import java.util.concurrent.BrokenBarrierException;
             import java.util.concurrent.CountDownLatch;
             import java.util.logging.Logger;
             import no.rmz.blobee.rpc.client.RpcClient;
             import no.rmz.blobee.rpc.server.RpcServer;
             import no.rmz.blobeeprototest.api.proto.Testservice;
             import no.rmz.blobeeprototest.api.proto.Testservice.RpcResult;
             import no.rmz.testtools.Net;


             /**
              * This really simple test sends messages over the loopback
              * wire a lot of times.  The intention of the test is both to
              * get a rough idea of the performance (at present about 0.02ms per
              * roundtrip on my laptop), but also to smoke out any memory leaks.
              *
              * The idea is simple: Perform a very simple invocation a lot of
              * times.  At present  100K, but it has proven to be
              * very convenient to try many different numbers (10K and 40K, 100K,  400K
              * 1M and 100M are favorites)in particular when smoking out blatant
              * performance and memory leaks.
              */
             public final class ReallySimplePerformanceTest {

                 /**
                  * When things go long we log.
                  */
                 private static final Logger log = Logger.getLogger(
                         no.rmz.blobee.rpc.RpcPeerInvocationTest.class.getName());
                 /**
                  * The number of iterations we should run during the test.
                  */
                 private final static int ROUNDTRIPS = 1000000;

                 /**
                  * The host where we set up the server.
                  */
                 private final static String HOST = "localhost";

                 /**
                  * The port we set up the server on.  It will be dynamically allocated
                  * by finding a free port and then just use that.
                  */
                 private int port;

                 /**
                  * The RpcChannel that is connected to the client and will be used
                  * to invoke the service.
                  */
                 private RpcChannel clientChannel;

                 /**
                  * The RpcClient that is used to sent messages over the channel.
                  */
                 private RpcClient rpcClient;

                 /**
                  * The server accepting incoming requests.
                  */
                 private  RpcServer rpcServer;

                 /**
                  * This test is all about simplicity and speed, so we pre-compute both
                  * the parameter and the response.  This is the request parameter, and
                  * it contains nothing in the way of payload.
                  */
                 private Testservice.RpcParam request = Testservice.RpcParam.newBuilder().build();

                 /**
                  * The message that is sent in the response.
                  */
                 public final static String RETURN_VALUE = "Going home";

                 /**
                  * A return value used to return results from the server serving the
                  * RPC requests.  It contains a short static string.
                  */
                 final RpcResult RETURNVALUE =
                         Testservice.RpcResult.newBuilder().setReturnvalue(RETURN_VALUE).build();


                 /**
                  * An RPC service implementation is made by first subclassing
                  * the abstract service class generated by protoc, in this case
                  * Testservice.RpcService, and then wiring it up in a server context.
                  * This is the implementation class, the wiring up happens further down.
                  */
                 public final class TestService extends Testservice.RpcService {

                     @Override
                     public void invoke(
                             final RpcController controller,
                             final Testservice.RpcParam request,
                             final RpcCallback<Testservice.RpcResult> done) {
                         // We just return a pecomputed return value.
                         done.run(RETURNVALUE);
                     }
                 }

                 /**
                  * This is where we set up the server and the client
                  * and start them up.
                  */
                 public void setUp()  {
                     try {
                         // First we find a free port
                         port = Net.getFreePort();
                     }
                     catch (IOException ex) {
                         throw new RuntimeException(ex);
                     }

                     // Then we set up a new server.
                     // This is done using a "cascading" style, so the server is
                     // actually created by the first line (newServer),
                     // then one or more service implementations are added
                     // (in this case one), and finally the service is started.
                     rpcServer =
                             RpcSetup.newServer(
                                 new InetSocketAddress(HOST, port))
                             .addImplementation(
                                 new TestService(), // An implementation instance
                                 Testservice.RpcService.Interface.class) // The service interface it implements.
                             .start(); // finally start the whole thing.

                     // Then we set up a client.  The pattern is much
                     // same as for servers, first we create the  instance,
                     // then we add a service implementation class that is handled by
                     // the client, and then we start it. Starting the client will
                     // involve connecting to the server at the other end.
                     rpcClient =
                             RpcSetup
                             .newClient(new InetSocketAddress(HOST, port))
                             .addInterface(Testservice.RpcService.class)
                             .start();

                     // Finally we get an RPC client that is actually used when
                     // invoking the RPC service (this is the way the RPC interface
                     // provided by the protoc compiler assumes we will use
                     // RPC).
                     clientChannel = rpcClient.newClientRpcChannel();
                 }

                 /**
                  * In this method we actually run the test.   It sets up a callback
                  * for the RPC, then invokes the service XXXX To be continued....
                  * @throws InterruptedException
                  * @throws BrokenBarrierException
                  */
                 @edu.umd.cs.findbugs.annotations.SuppressWarnings("WA_AWAIT_NOT_IN_LOOP")
                 public void testRpcInvocation() throws InterruptedException, BrokenBarrierException {

                     // The test is done when we've counted down the
                     // callbackCounter latch.
                     final CountDownLatch callbackCounter = new CountDownLatch(ROUNDTRIPS);

                     // The callback does the countdown
                     final RpcCallback<Testservice.RpcResult> callback =
                             new RpcCallback<Testservice.RpcResult>() {
                         public void run(final Testservice.RpcResult response) {
                             callbackCounter.countDown();
                         }
                     };

                     // We create a new RPC service based on the client channel
                     // we maede in the setup.
                     final Testservice.RpcService myService =
                             Testservice.RpcService.newStub(clientChannel);

                     // We do a bit of timing
                     final long startTime = System.currentTimeMillis();

                     // ... and let it rip.  Nothing magical here: We create a new controller
                     // per invocation(recycling is just too much hassle).
                     for (int i = 0; i < ROUNDTRIPS; i++) {
                         final RpcController clientController = rpcClient.newController();
                         myService.invoke(clientController, request, callback);
                     }

                     // Then we make an order of magnitude calculation about how
                     // long the user should expect this to take and inform her
                     // through the log.
                     final int marginFactor = 2;
                     final double expectedTime = 0.025 * ROUNDTRIPS * marginFactor;
                     final long expectedMillis = (long) expectedTime;
                     log.info("This shouldn't take more than "
                             + expectedMillis
                             + " millis (margin factor = "
                             + marginFactor
                             + ")");

                     // Now we wait.  We won't pass this barrier until all the
                     // invocations have returned to the callback
                     callbackCounter.await();

                     // So now we know how long it took, stop the stopwatch.
                     // and make some calculations.
                     final long endTime = System.currentTimeMillis();
                     final long duration = endTime - startTime;
                     final double millisPerRoundtrip = (double) duration / (double) ROUNDTRIPS;

                     // Then tell the user about our results.
                     log.info("Duration of "
                             + ROUNDTRIPS
                             + " iterations was "
                             + duration
                             + " milliseconds.  "
                             + millisPerRoundtrip
                             + " milliseconds per roundtrip.");
                     log.info("Latch count "
                             + callbackCounter.getCount());
                 }

                 /**
                  * Ignores all input parameters, runs the test only according to
                  * parameters defind in the class.
                  * @param argv
                  * @throws Exception
                  */
                 public final static void main(final String argv[])  throws Exception {
                     final ReallySimplePerformanceTest tst = new ReallySimplePerformanceTest();
                     tst.setUp();
                     tst.testRpcInvocation();

                     tst.rpcClient.stop();
                     tst.rpcServer.stop();
                 }
             }
