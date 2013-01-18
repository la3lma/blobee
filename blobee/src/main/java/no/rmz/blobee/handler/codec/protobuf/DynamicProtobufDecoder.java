/*
 * Copyright 2011 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package no.rmz.blobee.handler.codec.protobuf;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.protobuf.Message;
import com.google.protobuf.MessageLite;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.LengthFieldPrepender;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;

/**
 *
 * XXX NOTE: These are the original comments from the ProtobufDecoder that this
 * class is based on, not comments that actually describes what this class does.
 * A new set of comments should be written (rmz).
 *
 * Decodes a received {@link ChannelBuffer} into a <a
 * href="http://code.google.com/p/protobuf/">Google Protocol Buffers</a>
 * {@link Message} and {@link MessageLite}. Please note that this decoder must
 * be used with a proper {@link FrameDecoder} such as
 * {@link ProtobufVarint32FrameDecoder} or {@link LengthFieldBasedFrameDecoder}
 * if you are using a stream-based transport such as TCP/IP. A typical setup for
 * TCP/IP would be:
 * <pre>
 * {@link ChannelPipeline} pipeline = ...;
 *
 * // Decoders pipeline.addLast("frameDecoder", new
 * {@link LengthFieldBasedFrameDecoder}(1048576, 0, 4, 0, 4));
 * pipeline.addLast("protobufDecoder", new
 * {@link ProtobufDecoder}(MyMessage.getDefaultInstance()));
 *
 * // Encoder pipeline.addLast("frameEncoder", new
 * {@link LengthFieldPrepender}(4)); pipeline.addLast("protobufEncoder", new
 * {@link ProtobufEncoder}());
 * </pre> and then you can use a {@code MyMessage} instead of a
 * {@link ChannelBuffer} as a message:
 * <pre>
 * void messageReceived({@link ChannelHandlerContext} ctx, {@link MessageEvent}
 * e) { MyMessage req = (MyMessage) e.getMessage(); MyMessage res =
 * MyMessage.newBuilder().setText( "Did you say '" + req.getText() +
 * "'?").build(); ch.write(res); }
 * </pre>
 *
 * @apiviz.landmark
 */
@Sharable
public final class DynamicProtobufDecoder extends OneToOneDecoder {

    private final BlockingQueue<MessageLite> queue;

    public DynamicProtobufDecoder() {
         queue = new ArrayBlockingQueue<MessageLite>(1);
    }

    private MessageLite getNextPrototype() throws InterruptedException {
        return queue.take();
    }

    public void putNextPrototype(final MessageLite msg) {
        checkNotNull(msg);
        queue.add(msg);
    }

    @Override
    protected Object decode(
            final ChannelHandlerContext ctx,
            final Channel channel,
            final Object msg) throws Exception {

        if ( !(msg instanceof ChannelBuffer)) {
            return msg;
        }

        checkNotNull(channel);
        checkNotNull(msg);
        checkNotNull(ctx);

        final ChannelBuffer buf = (ChannelBuffer) msg;
        final MessageLite prototype = getNextPrototype();
        if (buf.hasArray()) {
            final int offset = buf.readerIndex();
            return prototype.newBuilderForType().mergeFrom(
                    buf.array(), buf.arrayOffset() + offset,
                    buf.readableBytes()).build();
        } else {
            return prototype.newBuilderForType().mergeFrom(
                    new ChannelBufferInputStream((ChannelBuffer) msg)).build();
        }
    }
}
