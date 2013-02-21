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

package no.rmz.blobee.rpc.client;

/**
 * Used for debugging only, used to listen in on invocations
 * that goes through the client side.
 */
public interface RpcClientSideInvocationListener {

    /**
     * When the client side sees an invocation, this method
     * lets a spy listen in on it.
     * @param invocation an invocation to spy on.
     */
     void listenToInvocation(final RpcClientSideInvocation invocation);
}
