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
package no.rmz.blobee.rpc.methods;

import static com.google.common.base.Preconditions.checkNotNull;
import java.lang.reflect.Method;

/**
 * A  value class descption of remotely invoked methods that is used
 * on the server side of an RPC connection.
 */
public final class ServerSideMethodDesc {

    /**
     * A method being invoked.
     */
    private final Method method;

    /**
     * The method's return type.
     */
    private final Class returnType;

    /**
     * The method's parameter type.
     */
    private final Class pmType;


    /**
     * Construct a new value object holding a description of an
     * RPC invoked method.  This description is only used on the
     * serving side of a connection.
     * @param method The method.
     * @param returnType The method's return type.
     * @param pmType  The method's parameter type.
     */
    public ServerSideMethodDesc(
           final Method method,
           final Class returnType,
           final Class pmType) {
        this.method = checkNotNull(method);
        this.returnType = checkNotNull(returnType);
        this.pmType = checkNotNull(pmType);
    }

    /**
     * The method.
     * @return  The method.
     */
    public Method getMethod() {
        return method;
    }


    /**
     * The method's return type.
     * @return  The return type of the method.
     */
    public Class getReturnType() {
        return returnType;
    }

    /**
     * The method's parameter type.
     * @return  The parameter type of the method.
     */
    public Class getPmType() {
        return pmType;
    }
}
