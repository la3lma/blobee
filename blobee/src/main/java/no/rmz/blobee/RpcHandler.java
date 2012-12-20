package no.rmz.blobee;

import no.rmz.blobeeproto.api.proto.Rpc;

public interface  RpcHandler {

    public Rpc.RpcResult invoke(Rpc.RpcParam param);

}
