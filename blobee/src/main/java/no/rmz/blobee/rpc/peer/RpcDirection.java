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

/**
 * Enum used to denote the direction an RPC is going.
 */
public enum RpcDirection {
    /**
     * An RPC call that is returning with a result from a previous
     * invocation.
     */
    RETURNING,

    /**
     * A new RPC call that expects to be first evaluated and then returned.
     */
    INVOKING
}
