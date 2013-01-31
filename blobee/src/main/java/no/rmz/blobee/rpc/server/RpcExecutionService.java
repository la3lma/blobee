/**
 * Copyright 2013 Bjørn Remseth (la3lma@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package no.rmz.blobee.rpc.server;

import java.lang.reflect.InvocationTargetException;
import no.rmz.blobee.rpc.peer.RemoteExecutionContext;
import no.rmz.blobeeproto.api.proto.Rpc;
import org.jboss.netty.channel.ChannelHandlerContext;

public interface RpcExecutionService {

    public Class getReturnType(final Rpc.MethodSignature sig);

    public Class getParameterType(final Rpc.MethodSignature sig);

    public void execute(RemoteExecutionContext dc, ChannelHandlerContext ctx, Object message);

    public void startCancel(ChannelHandlerContext ctx, long rpcIndex);

    public  void addImplementation(
            final Object implementation,
            final Class interfaceClass) throws SecurityException, IllegalStateException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, ExecutionServiceException;

}