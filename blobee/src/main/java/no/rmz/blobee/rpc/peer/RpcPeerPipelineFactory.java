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
package no.rmz.blobee.rpc.peer;

import static com.google.common.base.Preconditions.checkNotNull;
import no.rmz.blobee.rpc.client.MethodSignatureResolver;
import no.rmz.blobee.rpc.client.RpcClientFactory;
import no.rmz.blobee.rpc.server.RpcExecutionService;
import no.rmz.blobeeproto.api.proto.Rpc;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import static org.jboss.netty.channel.Channels.pipeline;
import org.jboss.netty.handler.codec.protobuf.ProtobufDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufEncoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;


public final class RpcPeerPipelineFactory implements ChannelPipelineFactory {
    /**
     * A name used to keep track of which pipeline factory this
     * is.
     */
    private final String name;

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

    // XXX Missing javadoc
    private final MethodSignatureResolver clientResolver;

    /**
     * An endpoint that can accept incoming requests for remote
     * procedure calls, and that can receive answers from our
     * peer at the other end of the wire when it has processed
     * the request.
     */
    private final RpcClientFactory rcf;


    public RpcPeerPipelineFactory(
            final String name,
            final RpcExecutionService executor,
            final RpcClientFactory rcf) {
        this.name = checkNotNull(name);
        this.executionService = checkNotNull(executor);
        this.rcf = checkNotNull(rcf);
        this.clientResolver = rcf.getResolver();
    }

    public RpcPeerPipelineFactory(
            final String name,
            final RpcExecutionService executor,
            final RpcClientFactory rcf,
            final RpcMessageListener listener) {
        this(name, executor, rcf);
        this.listener = listener;
    }


    // XXX Eventually this thing should get things like
    //     compression, ssl, http, whatever, but for not it's just
    //     the simplest possible pipeline I could get away with, and it's
    //     complex enough already.
    public ChannelPipeline getPipeline() throws Exception {
        final ProtobufDecoder protbufDecoder;
        protbufDecoder = new ProtobufDecoder(Rpc.RpcControl.getDefaultInstance());
        final ChannelPipeline p = pipeline();

        p.addLast("frameDecoder", new ProtobufVarint32FrameDecoder());
        p.addLast("protobufDecoder", protbufDecoder);
        p.addLast("frameEncoder", new ProtobufVarint32LengthFieldPrepender());
        p.addLast("protobufEncoder", new ProtobufEncoder());
        final RpcPeerHandler handler =
                new RpcPeerHandler(
                    clientResolver,
                    executionService,
                    rcf);

        if (listener != null) {
            handler.setListener(listener);
        }

        p.addLast("handler", handler);

        return p;
    }
}
