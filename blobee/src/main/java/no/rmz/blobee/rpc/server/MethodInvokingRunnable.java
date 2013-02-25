package no.rmz.blobee.rpc.server;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import no.rmz.blobee.controllers.RpcServiceController;
import no.rmz.blobee.controllers.RpcServiceControllerImpl;
import no.rmz.blobee.rpc.peer.RemoteExecutionContext;
import org.jboss.netty.channel.ChannelHandlerContext;


final class MethodInvokingRunnable implements Runnable {
    private final Object implementation;
    private final RemoteExecutionContext dc;
    private final ChannelHandlerContext ctx;
    private final Object parameter;
    private final RpcExecutionServiceImpl executor;
    private final boolean noReturn;
    private final boolean multiReturn;

    public MethodInvokingRunnable(
            final Object implementation,
            final RemoteExecutionContext dc,
            final ChannelHandlerContext ctx,
            final Object parameter,
            final ControllerStorage ctStor,
            final RpcExecutionServiceImpl executor,
            final boolean multiReturn,
            final boolean noReturn) {
        this.implementation = checkNotNull(implementation);
        this.dc = checkNotNull(dc);
        this.ctx = checkNotNull(ctx);
        this.parameter = checkNotNull(parameter);
        this.executor = checkNotNull(executor);
        this.multiReturn = multiReturn;
        this.noReturn = noReturn;
    }

    private final Object monitor = new Object();

    private boolean hasReturnedOnce = false;

    @Override
    public void run() {
        final Method method = executor.getMethod(dc.getMethodSignature());
        final RpcServiceController controller =
                new RpcServiceControllerImpl(dc, multiReturn, noReturn);
        executor.storeController(ctx, dc.getRpcIndex(), controller);
        final RpcCallback<Message> callbackAdapter =
                new RpcCallback<Message>() {
                    @Override
                    public void run(final Message response) {
                        if (hasReturnedOnce && !multiReturn) {
                            throw new IllegalMultiReturnException(MethodInvokingRunnable.this, this);
                        } else {
                            hasReturnedOnce = true;
                        }

                        controller.invokeCancelledCallback();
                        dc.returnResult(response);
                    }
        };
        try {
            method.invoke(implementation, controller,
                          parameter, callbackAdapter);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new RuntimeException(ex);
        } finally {
            if (multiReturn) {
                dc.terminateMultiReturnSequence();
            }
            executor.removeController(ctx, dc.getRpcIndex());
        }
    }
}
