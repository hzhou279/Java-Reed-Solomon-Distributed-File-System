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
package edu.cmu.reedsolomonfs.server;

import edu.cmu.reedsolomonfs.datatype.FileMetadata;

/**
 * The counter service supporting query and count function.
 *
 * @author likun (saimu.msm@antfin.com)
 */
public interface ChunkserverService {

    /**
     * Get current value from counter
     *
     * Provide consistent reading if {@code readOnlySafe} is true.
     */
    void get(final boolean readOnlySafe, final ChunkserverClosure closure);

    /**
     * Add delta to counter then get value
     */
    void incrementAndGet(final long delta, final ChunkserverClosure closure);

    /**
     * SetBytesValue
     */
    void setBytesValue(final byte[] bytes, final ChunkserverClosure closure);

    /**
     * Write data from client to cluster
     */
    void write(final byte[][] shards, final FileMetadata metadata, final ChunkserverClosure closure);


    /**
     * Read data from cluster to client
     */
    void read(String filePath, final ChunkserverClosure closure);
}