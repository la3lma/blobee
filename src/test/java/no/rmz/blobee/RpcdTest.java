package no.rmz.blobee;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public final class RpcdTest {

    /**
     * We need a dummy key so this is it. However, it does allude to the fact
     * that we also do need some kind of namespace to identify handlers, but for
     * now this is just a placeholder.
     */
    private static final String RPC_KEY = "foo.bar.baz";
    /**
     * We need some place to stash the result of an invocation, and this is it.
     */
    private RpcResult rpcResult;
    /**
     * An RPC demon we use for testing.
     */
    private Rpcd rpcd;
    /**
     * true iff an invocation actually happened.
     */
    private boolean invocationHappened = false;

    @Before
    public void setUp() {
        rpcResult = null;
        invocationHappened = false;
        rpcd = new Rpcd();

    }

    @Test
    public void testAddingAHandler() throws RpcdException {
        assertFalse(rpcd.hasHandlerForKey(RPC_KEY));
        rpcd.register(RPC_KEY, new RpcHandler() {
            public RpcResult invoke(final RpcParam param) {
                return null;
            }
        });
        assertTrue(rpcd.hasHandlerForKey(RPC_KEY));
    }

    @Test(expected = RpcdException.class)
    public void testAddingATwoHandlersForTheSameKey() throws RpcdException {
        final Rpcd rpcd = new Rpcd();

        // We expect this first invocation to succeed.
        rpcd.register(RPC_KEY, new RpcHandler() {
            public RpcResult invoke(final RpcParam param) {
                return null;
            }
        });

        // We expect this second invocation to fail.
        rpcd.register(RPC_KEY, new RpcHandler() {
            public RpcResult invoke(final RpcParam param) {
                return null;
            }
        });
    }

    public void testThatInvocationHappens() throws RpcdException {

        assertFalse(rpcd.hasHandlerForKey(RPC_KEY));
        rpcd.register(RPC_KEY, new RpcHandler() {
            public RpcResult invoke(final RpcParam param) {
                return null;
            }
        });


        invocationHappened = false;
        // XXX This is where we want to steal API design
        //     from the java execution things, so once this
        //     passes some minimal functional test, that's what we
        //     should do.
        rpcd.invoke(RPC_KEY,
                new RpcParam() {
                },
                new RpcResultHandler() {
                    public void receiveResult(final RpcResult result) {
                        invocationHappened = true;
                    }
                });

        assertTrue(invocationHappened);
    }

    public void testInvokingWithMissingHandler() throws RpcdException {


        // XXX This is where we want to steal API design
        //     from the java execution things, so once this
        //     passes some minimal functional test, that's what we
        //     should do.
        rpcd.invoke(RPC_KEY,
                new RpcParam() {
                },
                new RpcResultHandler() {
                    public void receiveResult(final RpcResult result) {
                        invocationHappened = true;
                        rpcResult = result;
                    }
                });
        assertTrue(invocationHappened);
        assertEquals(RpcStatusCode.NO_HANDLER, rpcResult);
    }

    public void testNullReturningHandler() throws RpcdException {


        rpcd.register(RPC_KEY, new RpcHandler() {
            public RpcResult invoke(final RpcParam param) {
                return null;
            }
        });

        rpcd.invoke(RPC_KEY,
                new RpcParam() {
                },
                new RpcResultHandler() {
                    public void receiveResult(final RpcResult result) {
                        invocationHappened = true;
                        rpcResult = result;
                    }
                });
        assertTrue(invocationHappened);
        assertEquals(RpcStatusCode.HANDLER_FAILURE, rpcResult);
    }

    public void testContentProducingHandler() throws RpcdException {


        rpcd.register(RPC_KEY, new RpcHandler() {
            public RpcResult invoke(final RpcParam param) {
                return new RpcResult(RpcStatusCode.OK);
            }
        });

        rpcd.invoke(RPC_KEY,
                new RpcParam() {
                },
                new RpcResultHandler() {
                    public void receiveResult(final RpcResult result) {
                        invocationHappened = true;
                        rpcResult = result;
                    }
                });
        assertTrue(invocationHappened);
        assertEquals(RpcStatusCode.HANDLER_FAILURE, rpcResult);
    }
}
