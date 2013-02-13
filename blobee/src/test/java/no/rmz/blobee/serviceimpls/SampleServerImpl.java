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
package no.rmz.blobee.serviceimpls;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import no.rmz.blobeetestproto.api.proto.Testservice.RpcParam;
import no.rmz.blobeetestproto.api.proto.Testservice.RpcResult;
import no.rmz.blobeetestproto.api.proto.Testservice.RpcService;


public  final class SampleServerImpl extends RpcService {

    public static final String RETURN_VALUE = "Going home";

    private final RpcResult result =
            RpcResult.newBuilder().setReturnvalue(RETURN_VALUE).build();

    @Override
    public void invoke(
            final RpcController controller,
            final RpcParam request,
            final RpcCallback<RpcResult> done) {
        done.run(result);
    }
}
