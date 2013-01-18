/**
 * Copyright 2013  Bjørn Remseth (la3lma@gmail.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
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
    /**
     * A name used to keep track of which pipeline factory this
     * is.
     */
    private final String name;

    // XXX
    private final WeakHashMap<ChannelPipeline, DynamicProtobufDecoder> decoderMap =
            new WeakHashMap<ChannelPipeline, DynamicProtobufDecoder>();

    /**
     * For debugging purposes we can insert a listener
     * that can listen in to the messages that arrives at this
     * RpcPeer.
     */
    private RpcMessageListener listener;

    /**
     * A service that is used to execute incoming requests for
     * remote procedure calls.
     */
    private final RpcExecutionService executionService;

    /**
     * An endpoint that can accept incoming requests for remote
     * procedure calls, and that can receive answers from our
     * peer at the other end of the wire when it has processed
     * the request.
     */
    private final RpcClient rpcClient;

    public RpcPeerPipelineFactory(
            final String name,
            final RpcExecutionService executionService,
            final RpcClient rpcClient) {
        this(name, executionService, rpcClient, null);
    }

    protected RpcPeerPipelineFactory(
            final String name,
            final RpcExecutionService executionService,
            final RpcClient rpcClient,
            final RpcMessageListener listener) {
        this.name = checkNotNull(name);
        this.executionService = checkNotNull(executionService);
        this.rpcClient = checkNotNull(rpcClient);
        this.listener = listener;
    }

    public void putNextPrototype(final ChannelPipeline pipeline, final MessageLite prototype) {
        if (decoderMap.containsKey(pipeline)) {
            decoderMap.get(pipeline).putNextPrototype(prototype.getDefaultInstanceForType());
        } else {
            // XXX Don't use runtime exception, use a checkable exception
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
        final RpcPeerHandler handler =
                new RpcPeerHandler(protbufDecoder, executionService, rpcClient);
        if (listener != null) {
            handler.setListener(listener);
        }
        p.addLast("handler", handler);

        // The first message to receive is always a control message,
        // so we set the pipeline up to expect that.
        putNextPrototype(p, Rpc.RpcControl.getDefaultInstance());
        return p;
    }
}
