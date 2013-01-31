package no.rmz.blobee.rpc;

import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import no.rmz.blobee.controllers.RpcServiceController;

public interface ExecutionServiceListener {


    public void listen(
            ExecutorService threadPool,
            Object object,
            Object implementation,
            Object object0,
            Object parameter,
            Object object1);

}
