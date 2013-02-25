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
package no.rmz.blobee.rpc.server;

import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;

/**
 * Thrown when a multi return is attempted in a setting where it
 * is not permitted.  Needs to be unchecked since it has to pass through
 * a Google/protoc defined API that don't throw any checked exception.
 */
public class IllegalReturnException extends RuntimeException {

    public IllegalReturnException(
            final String msg,
            final MethodInvokingRunnable runnable,
            final RpcCallback<Message> callback) {
       super("msg = " + msg
               + ", runnable  = " + runnable
               + ", callback = " + callback);
    }


}
