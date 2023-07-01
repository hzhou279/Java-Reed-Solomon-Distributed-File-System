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

import java.io.Serializable;

/**
 * The counter operation
 *
 * @author likun (saimu.msm@antfin.com)
 */
public class ChunkserverOperation implements Serializable {

    private static final long serialVersionUID = -6597003954824547294L;

    /** Get value */
    public static final byte  GET              = 0x01;
    /** Increment and get value */
    public static final byte  INCREMENT        = 0x02;
    /** Write bytes */
    public static final byte  WRITE_BYTES      = 0x03;
    /** Read bytes */
    public static final byte  READ_BYTES       = 0x04;

    private byte              op;
    private long              delta;
    private byte[]            bytes;
    private byte[][]          shards;

    public static ChunkserverOperation createGet() {
        return new ChunkserverOperation(GET);
    }

    public static ChunkserverOperation createIncrement(final long delta) {
        return new ChunkserverOperation(INCREMENT, delta);
    }

    public static ChunkserverOperation createSetBytesValue(final byte[] bytes) {
        return new ChunkserverOperation(WRITE_BYTES, bytes);
    }

    public static ChunkserverOperation createWrite(final byte[][] shards) {
        return new ChunkserverOperation(WRITE_BYTES, shards);
    }

    public static ChunkserverOperation createReadBytes() {
        return new ChunkserverOperation(READ_BYTES);
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

    public ChunkserverOperation(byte op, byte[][] shards) {
        this.op = op;
        this.shards = shards;
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

    public boolean isReadOp() {
        return GET == this.op;
    }
}
