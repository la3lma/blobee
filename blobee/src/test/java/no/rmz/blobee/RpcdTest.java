package no.rmz.blobee;

import no.rmz.blobeeproto.api.proto.Rpc;
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
     * An RPC demon we use for testing.
     */
    private Rpcd rpcd;
    /**
     * true iff an invocation actually happened.
     */
    private boolean invocationHappened = false;

    @Before
    public void setUp() {
        invocationHappened = false;
        rpcd = new Rpcd();

    }

    @Test
    public void testAddingAHandler() throws RpcdException {
        assertFalse(rpcd.hasHandlerForKey(RPC_KEY));
        rpcd.register(RPC_KEY, new RpcHandler() {
            public Rpc.RpcResult invoke(final Rpc.RpcParam param) {
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
            public  Rpc.RpcResult  invoke(final Rpc.RpcParam param) {
                return null;
            }
        });

        // We expect this second invocation to fail.
        rpcd.register(RPC_KEY, new RpcHandler() {
            public  Rpc.RpcResult  invoke(final Rpc.RpcParam param) {
                return null;
            }
        });
    }

    @Test
    public void testThatInvocationHappens() throws RpcdException {

        assertFalse(rpcd.hasHandlerForKey(RPC_KEY));
        rpcd.register(RPC_KEY, new RpcHandler() {
            public  Rpc.RpcResult  invoke(final Rpc.RpcParam param) {
                return null;
            }
        });


        invocationHappened = false;
        // XXX This is where we want to steal API design
        //     from the java execution things, so once this
        //     passes some minimal functional test, that's what we
        //     should do.
        rpcd.invoke(RPC_KEY,
                Rpc.RpcParam.newBuilder().build(),
                new RpcResultHandler() {
                    public void receiveResult(final Rpc.RpcResult result) {
                        invocationHappened = true;
                    }
                });

        assertTrue(invocationHappened);
    }

    @Test
    public void testInvokingWithMissingHandler() throws RpcdException {


        // XXX This is where we want to steal API design
        //     from the java execution things, so once this
        //     passes some minimal functional test, that's what we
        //     should do.
        rpcd.invoke(RPC_KEY,
                Rpc.RpcParam.newBuilder().build(),
                new RpcResultHandler() {
                    public void receiveResult(final Rpc.RpcResult result) {
                        invocationHappened = true;
                        assertEquals(Rpc.RpcResult.newBuilder().setStat(Rpc.RpcResult.StatusCode.NO_HANDLER).build(),
                                result);
                    }
                });
        assertTrue(invocationHappened);

    }

    @Test
    public void testNullReturningHandler() throws RpcdException {


        rpcd.register(RPC_KEY, new RpcHandler() {
            public  Rpc.RpcResult  invoke(final Rpc.RpcParam param) {
                return null;
            }
        });

        rpcd.invoke(RPC_KEY,
                Rpc.RpcParam.newBuilder().build(),
                new RpcResultHandler() {
                    public void receiveResult(final Rpc.RpcResult result) {
                        invocationHappened = true;
                        assertEquals(
                                Rpc.RpcResult.newBuilder().setStat(Rpc.RpcResult.StatusCode.HANDLER_FAILURE).build(), result);
                    }
                });
        assertTrue(invocationHappened);
    }

    @Test
    public void testContentProducingHandler() throws RpcdException {


        rpcd.register(RPC_KEY, new RpcHandler() {
            public  Rpc.RpcResult  invoke(final Rpc.RpcParam param) {
                return Rpc.RpcResult.newBuilder().setStat(Rpc.RpcResult.StatusCode.OK).build();
            }
        });

        rpcd.invoke(RPC_KEY,
                Rpc.RpcParam.newBuilder().build(),
                new RpcResultHandler() {
                    public void receiveResult(final Rpc.RpcResult result) {
                        invocationHappened = true;
                        assertEquals(Rpc.RpcResult.newBuilder().setStat(Rpc.RpcResult.StatusCode.OK).build(), result);
                    }
                });
        assertTrue(invocationHappened);
    }
}
