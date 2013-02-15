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


public final class ServerSideMethodDesc {
    private final Method method;
    private final Class returnType;
    private final Class pmType;


    public ServerSideMethodDesc(
           final Method method,
           final Class returnType,
           final Class pmType) {
        this.method = checkNotNull(method);
        this.returnType = checkNotNull(returnType);
        this.pmType = checkNotNull(pmType);
    }

    public Method getMethod() {
        return method;
    }

    public Class getReturnType() {
        return returnType;
    }

    public Class getPmType() {
        return pmType;
    }
}
