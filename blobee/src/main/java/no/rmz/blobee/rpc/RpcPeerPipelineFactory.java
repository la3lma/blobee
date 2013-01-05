package no.rmz.blobee.rpc;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.protobuf.MessageLite;
import java.util.WeakHashMap;
import no.rmz.blobee.handler.codec.protobuf.DynamicProtobufDecoder;
import no.rmz.blobeeproto.api.proto.Rpc;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import static org.jboss.netty.channel.Channels.pipeline;
import org.jboss.netty.handler.codec.protobuf.ProtobufEncoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;


public final class RpcPeerPipelineFactory implements ChannelPipelineFactory {
    private final String name;
    private final WeakHashMap<ChannelPipeline, DynamicProtobufDecoder> decoderMap =
            new WeakHashMap<ChannelPipeline, DynamicProtobufDecoder>();
    private RpcMessageListener listener;

    public RpcPeerPipelineFactory(final String name) {
        this.name = checkNotNull(name);
    }

    protected RpcPeerPipelineFactory(
            final String name,
            final RpcMessageListener listener) {
        this.name = checkNotNull(name);
        this.listener = listener;
    }

    public void putNextPrototype(final ChannelPipeline pipeline, final MessageLite prototype) {
        if (decoderMap.containsKey(pipeline)) {
            decoderMap.get(pipeline).putNextPrototype(prototype.getDefaultInstanceForType());
        } else {
            throw new RuntimeException("This is awful");
        }
    }

    // XXX Eventually this thing should get things like
    //     compression, ssl, http, whatever, but for not it's just
    //     the simplest possible pipeline I could get away with, and it's
    //     complex enough already.
    public ChannelPipeline getPipeline() throws Exception {
        final DynamicProtobufDecoder protbufDecoder = new DynamicProtobufDecoder();
        final ChannelPipeline p = pipeline();
        decoderMap.put(p, protbufDecoder);
        p.addLast("frameDecoder", new ProtobufVarint32FrameDecoder());
        p.addLast("protobufDecoder", protbufDecoder);
        p.addLast("frameEncoder", new ProtobufVarint32LengthFieldPrepender());
        p.addLast("protobufEncoder", new ProtobufEncoder());
        final RpcPeerHandler handler = new RpcPeerHandler(protbufDecoder);
        if (listener != null) {
            handler.setListener(listener);
        }
        p.addLast("handler", handler);
        putNextPrototype(p, Rpc.RpcControl.getDefaultInstance());
        return p;
    }

}
