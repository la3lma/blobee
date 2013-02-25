/*
 * Copyright 2013 rmz.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package no.rmz.blobee.rpc.client;

import com.google.protobuf.RpcController;

/**
 * An interface extending the standard RpcController interface in ways that are
 * necessary to make Blobee into an actually useful RPC implementation.
 */
public interface BlobeeRpcController extends RpcController {

    /**
     * Set this invocation to be multi-return, meaning that a procedure can
     * return more than one value.
     */
    void setMultiReturn();

    /**
     * True iff the invocation is multi-return.
     *
     * @return true if invocation is multi-return.
     */
    boolean isMultiReturn();

    /**
     * Set this invocation to be no-return, meaning that it is assumed that the
     * return-value callback will not be invoked. Indeed it will represent an
     * error situation to attempt to use the return callback.
     */
    void setNoReturn();

    /**
     * True iff the invocation is a no-return invocation.
     *
     * @return true if invocation is no-return.
     */
    boolean isNoReturn();
}
