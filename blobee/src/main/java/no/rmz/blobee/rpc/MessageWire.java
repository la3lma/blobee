package no.rmz.blobee.rpc;

import com.google.protobuf.Message;

public interface MessageWire {

    public void write(Message msg1, Message msg2);

    public void write(Message msg1);

}
