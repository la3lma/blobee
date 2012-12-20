
package no.rmz.blobee;

import no.rmz.blobeeproto.api.proto.Rpc;

public interface  RpcResultHandler {

    public void receiveResult(final Rpc.RpcResult result);
}
