package no.rmz.blobee;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Descriptors.ServiceDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcChannel;
import com.google.protobuf.RpcController;
import com.google.protobuf.Service;
import java.util.logging.Logger;
import no.rmz.blobeeproto.api.proto.Rpc;
import no.rmz.blobeeproto.api.proto.Rpc.RpcParam;
import no.rmz.blobeeproto.api.proto.Rpc.RpcResult;
import no.rmz.blobeeproto.api.proto.Rpc.RpcService.Interface;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public final class RpcdTest {

    private final static Logger log = Logger.getLogger(RpcdTest.class.getName());
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

    //
    // Convenience methods for generating return values
    //
    private Rpc.RpcResult newResult(final Rpc.StatusCode code) {
        return Rpc.RpcResult.newBuilder().setStat(code).build();
    }

    private Rpc.RpcResult newNoHandler() {
        return newResult(Rpc.StatusCode.NO_HANDLER);
    }

    private Rpc.RpcResult newOk() {
        return newResult(Rpc.StatusCode.OK);
    }

    private Rpc.RpcResult newFailure() {
        return newResult(Rpc.StatusCode.HANDLER_FAILURE);
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
            public Rpc.RpcResult invoke(final Rpc.RpcParam param) {
                return null;
            }
        });

        // We expect this second invocation to fail.
        rpcd.register(RPC_KEY, new RpcHandler() {
            public Rpc.RpcResult invoke(final Rpc.RpcParam param) {
                return null;
            }
        });
    }

    @Test
    public void testThatInvocationHappens() throws RpcdException {

        assertFalse(rpcd.hasHandlerForKey(RPC_KEY));
        rpcd.register(RPC_KEY, new RpcHandler() {
            public Rpc.RpcResult invoke(final Rpc.RpcParam param) {
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
                        assertEquals(newNoHandler(),
                                result);
                    }
                });
        assertTrue(invocationHappened);

    }

    @Test
    public void testNullReturningHandler() throws RpcdException {


        rpcd.register(RPC_KEY, new RpcHandler() {
            public Rpc.RpcResult invoke(final Rpc.RpcParam param) {
                return null;
            }
        });

        rpcd.invoke(RPC_KEY,
                Rpc.RpcParam.newBuilder().build(),
                new RpcResultHandler() {
                    public void receiveResult(final Rpc.RpcResult result) {
                        invocationHappened = true;
                        assertEquals(
                                newFailure(), result);
                    }
                });
        assertTrue(invocationHappened);
    }

    @Test
    public void testContentProducingHandler() throws RpcdException {

        rpcd.register(RPC_KEY, new RpcHandler() {
            public Rpc.RpcResult invoke(final Rpc.RpcParam param) {
                return newOk();
            }
        });

        rpcd.invoke(RPC_KEY,
                Rpc.RpcParam.newBuilder().build(),
                new RpcResultHandler() {
                    public void receiveResult(final Rpc.RpcResult result) {
                        invocationHappened = true;
                        assertEquals(newOk(), result);
                    }
                });
        assertTrue(invocationHappened);
    }
}
