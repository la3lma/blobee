package no.rmz.blobee.rpc;

import static com.google.common.base.Preconditions.checkNotNull;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;

public final class AnnotationReadingRpcExecutionServiceImpl implements RpcExecutionService {

    private static final Logger log = Logger.getLogger(AnnotationReadingRpcExecutionServiceImpl.class.getName());
    final RpcExecutionServiceImpl imp;

    public AnnotationReadingRpcExecutionServiceImpl(
            final Object implementation) throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        imp = new RpcExecutionServiceImpl();
        checkNotNull(implementation);
        ServiceAnnotationMapper.bindServices(implementation, imp.getMethodMap());
    }

    @Override
    public void execute(
            final RemoteExecutionContext dc,
            final ChannelHandlerContext ctx,
            final Object param) {

        imp.execute(dc, ctx, param);
    }
}
