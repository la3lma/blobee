package no.rmz.blobee;

import static org.junit.Assert.*;
import org.junit.Test;

public final class RpcdTest {

    /**
     * We need a dummy key so this is it. However, it does allude to the fact
     * that we also do need some kind of namespace to identify handlers, but
     * for now this is just a placeholder.
     */
    private static final  String RPC_KEY = "foo.bar.baz";

    @Test
    public void testAddingAHandler() throws RpcdException {
        final Rpcd rpcd = new Rpcd();

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
}
