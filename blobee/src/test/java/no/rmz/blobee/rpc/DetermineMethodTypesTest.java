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
package no.rmz.blobee.rpc;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Message;
import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Logger;
import static com.google.common.base.Preconditions.checkNotNull;
import no.rmz.blobee.serviceimpls.SampleServerImpl;
import no.rmz.blobeeprototest.api.proto.Testservice;
import no.rmz.blobeeprototest.api.proto.Testservice.RpcParam;
import no.rmz.blobeeprototest.api.proto.Testservice.RpcResult;
import no.rmz.blobeeprototest.api.proto.Testservice.RpcService;
import org.junit.Test;
import org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public final class DetermineMethodTypesTest {

    private static final Logger log = Logger.getLogger(
            no.rmz.blobee.rpc.DetermineMethodTypesTest.class.getName());

    @Test
    public void determineMethodTypesTest() {

        final Testservice.RpcService service = (Testservice.RpcService) new SampleServerImpl();

        final Descriptors.ServiceDescriptor descriptor = service.getDescriptorForType();
        final List<Descriptors.MethodDescriptor> methods = descriptor.getMethods();
        final Method[] methods1 = service.getClass().getMethods();

        // XXX Linear search to get methods is not very efficient, but hey,
        //     it works.
        Method getRequestPrototype = null;
        for (Method m: methods1) {
            if (m.getName().equals("getRequestPrototype")) {
                getRequestPrototype = m;
                break;
            }
        }

        org.junit.Assert.assertNotNull(getRequestPrototype);
         Method getResponsePrototype = null;
        for (Method m: methods1) {
            if (m.getName().equals("getResponsePrototype")) {
                getResponsePrototype = m;
                break;
            }
        }
        org.junit.Assert.assertNotNull(getResponsePrototype);


        org.junit.Assert.assertEquals(1, methods.size());

        Descriptors.MethodDescriptor md = methods.get(0);


        final Message inputType = getType(service, getRequestPrototype, md);
        org.junit.Assert.assertNotNull(inputType);
        final Message outputType = getType(service, getResponsePrototype, md);
         org.junit.Assert.assertNotNull(outputType);

        final String fullName = md.getFullName();
        // XXX Why do I only get completely useless types from these
        //     two queries.  The results are in no way useful for
        //     deserialization.  It seeems I just can't get the proper
        //     types out of the Google code.  But why?
        /*
        final Message inputType = md.getInputType().toProto().getDefaultInstance();
        final Message outputType = md.getOutputType().toProto().getDefaultInstance();
*/
        org.junit.Assert.assertTrue(inputType instanceof Testservice.RpcParam);
        org.junit.Assert.assertTrue(outputType instanceof Testservice.RpcResult);
    }

    private Message getType(final Object instance, final Method method,  final MethodDescriptor md) {
        checkNotNull(instance);
        checkNotNull(method);
        checkNotNull(md);
        try {
            return (Message) method.invoke(instance, md);
        } catch (Exception ex) { /// XXXX
            org.junit.Assert.fail("Caught an exception: " + ex);
            return null; /// XXX Will never happen
        }
    }
}
