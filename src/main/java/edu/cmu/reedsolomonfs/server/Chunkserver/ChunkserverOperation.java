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

import java.io.Serializable;

import edu.cmu.reedsolomonfs.datatype.FileMetadata;

// The ChunkserverOperation class is referenced from 
// https://github.com/sofastack/sofa-jraft/blob/19ed179e02ee9108adc0bbf66badb47f62c62af8/jraft-example/src/main/java/com/alipay/sofa/jraft/example/counter/CounterOperation.java
public class ChunkserverOperation implements Serializable {

    private static final long serialVersionUID = -6597003954824547294L;

    /** Get value */
    public static final byte GET = 0x01;
    /** Increment and get value */
    public static final byte INCREMENT = 0x02;
    /** Write bytes */
    public static final byte WRITE_BYTES = 0x03;
    /** Read bytes */
    public static final byte READ_BYTES = 0x04;
    /** Delete bytes */
    public static final byte DELETE_BYTES = 0x05;

    public static final byte UPDATE_SECRETKEY = 0x06;

    private byte op;
    private long delta;
    private byte[] bytes;
    private byte[][] shards;
    private FileMetadata metadata;
    private String filePath;

    public static ChunkserverOperation createGet() {
        return new ChunkserverOperation(GET);
    }

    public static ChunkserverOperation createIncrement(final long delta) {
        return new ChunkserverOperation(INCREMENT, delta);
    }

    public static ChunkserverOperation createSetBytesValue(final byte[] bytes) {
        return new ChunkserverOperation(WRITE_BYTES, bytes);
    }

    public static ChunkserverOperation createWrite(final byte[][] shards, final FileMetadata metadata) {
        return new ChunkserverOperation(WRITE_BYTES, shards, metadata);
    }

    public static ChunkserverOperation createDelete(final String filePath) {
        return new ChunkserverOperation(DELETE_BYTES, filePath);
    }

    public static ChunkserverOperation createReadBytes() {
        return new ChunkserverOperation(READ_BYTES);
    }

    public static ChunkserverOperation updateSecretKey(final String secretKey) {
        System.out.println("update secret key operation");
        return new ChunkserverOperation(UPDATE_SECRETKEY, secretKey);
    }

    public ChunkserverOperation(byte op) {
        this(op, 0);
    }

    public ChunkserverOperation(byte op, long delta) {
        this.op = op;
        this.delta = delta;
    }

    public ChunkserverOperation(byte op, byte[] bytes) {
        this.op = op;
        this.bytes = bytes;
    }

    public ChunkserverOperation(byte op, byte[][] shards, FileMetadata metadata) {
        this.op = op;
        this.shards = shards;
        this.metadata = metadata;
    }

    public ChunkserverOperation(byte op, String filePath) {
        this.op = op;
        this.filePath = filePath;
    }

    public byte getOp() {
        return op;
    }

    public long getDelta() {
        return delta;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public byte[][] getShards() {
        return shards;
    }

    public FileMetadata getMetadata() {
        return metadata;
    }

    public String getFilePath() {
        return filePath;
    }

    public boolean isReadOp() {
        return GET == this.op;
    }
}
