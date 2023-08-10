/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.cmu.reedsolomonfs.server.Chunkserver;

import java.util.Map;

import com.alipay.sofa.jraft.Closure;
import com.google.protobuf.ByteString;

import edu.cmu.reedsolomonfs.server.ChunkserverOutter.ValueResponse;

// The ChunkserverClosure class is referenced from 
// https://github.com/sofastack/sofa-jraft/blob/19ed179e02ee9108adc0bbf66badb47f62c62af8/jraft-example/src/main/java/com/alipay/sofa/jraft/example/counter/CounterClosure.java
public abstract class ChunkserverClosure implements Closure {
    private ValueResponse    valueResponse;
    private ChunkserverOperation counterOperation;

    public void setCounterOperation(ChunkserverOperation counterOperation) {
        this.counterOperation = counterOperation;
    }

    public ChunkserverOperation getCounterOperation() {
        return counterOperation;
    }

    public ValueResponse getValueResponse() {
        return valueResponse;
    }

    public void setValueResponse(ValueResponse valueResponse) {
        this.valueResponse = valueResponse;
    }

    protected void failure(final String errorMsg, final String redirect) {
        final ValueResponse response = ValueResponse.newBuilder().setSuccess(false).setErrorMsg(errorMsg)
            .setRedirect(redirect).build();
        setValueResponse(response);
    }

    protected void success(final long value) {
        final ValueResponse response = ValueResponse.newBuilder().setValue(value).setSuccess(true).build();
        setValueResponse(response);
    }

    protected void successWithRead(final Map<String, byte[]> bytesMap) {
        final ValueResponse.Builder builder = ValueResponse.newBuilder();

        // print out the bytesMap
        System.out.println("bytesMap: " + bytesMap);

        for (final Map.Entry<String, byte[]> entry : bytesMap.entrySet()) {
            final String key = entry.getKey();
            final byte[] bytes = entry.getValue();
            if (bytes != null && bytes.length != 0) {
                builder.putChunkDataMap(key, ByteString.copyFrom(bytes));
            }
        }

        builder.setSuccess(true);
        builder.setValue(1);

        final ValueResponse response = builder.build();
        setValueResponse(response);
    }
}
