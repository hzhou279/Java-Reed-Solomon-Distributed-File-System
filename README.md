# reed-solomon-code-distributed-file-system

RSFS Design Document

[Project Timeline  2](https://docs.google.com/document/d/10ym4UC3V6ubWmCnKaPS0_6sFJ-XQCXoc3SkbCA7Iq14/edit#heading=h.isnay1sxloz4)

[Non-Functional Requirements Document  2](https://docs.google.com/document/d/10ym4UC3V6ubWmCnKaPS0_6sFJ-XQCXoc3SkbCA7Iq14/edit#heading=h.n9mzcgsm28xh)

[Entities: Role, state, storage, failure recovery plan  2](https://docs.google.com/document/d/10ym4UC3V6ubWmCnKaPS0_6sFJ-XQCXoc3SkbCA7Iq14/edit#heading=h.rbgvesdl0i3p)

[System Architecture and Interaction  3](https://docs.google.com/document/d/10ym4UC3V6ubWmCnKaPS0_6sFJ-XQCXoc3SkbCA7Iq14/edit#heading=h.a35wgqqjxlkg)

[System Interaction Diagrams of Write and Read Operations  3](https://docs.google.com/document/d/10ym4UC3V6ubWmCnKaPS0_6sFJ-XQCXoc3SkbCA7Iq14/edit#heading=h.ude5jtfgxliy)

[Division of responsibilities among participants  4](https://docs.google.com/document/d/10ym4UC3V6ubWmCnKaPS0_6sFJ-XQCXoc3SkbCA7Iq14/edit#heading=h.5lnk2jz2dob9)

[Library/Framework choices  5](https://docs.google.com/document/d/10ym4UC3V6ubWmCnKaPS0_6sFJ-XQCXoc3SkbCA7Iq14/edit#heading=h.5s4lkr8lcaa0)

[Client authentication  5](https://docs.google.com/document/d/10ym4UC3V6ubWmCnKaPS0_6sFJ-XQCXoc3SkbCA7Iq14/edit#heading=h.i8q2alihaipk)

[APIs Design  5](https://docs.google.com/document/d/10ym4UC3V6ubWmCnKaPS0_6sFJ-XQCXoc3SkbCA7Iq14/edit#heading=h.bhla3a18u4tc)

Project Timeline
----------------

<https://docs.google.com/spreadsheets/d/1x09mZnQb_mkT2ZK2kdasAY9iJgZhcIJcIqE92R2_geg/edit#gid=0>

Non-Functional Requirements Document
------------------------------------

1.  Using a single VM for all backend services (master and storage cluster). The VM has 8 cpu cores, 32GB memory and 100GB disk space.

2.  There will be a total of 7 containers. Master server will run in a container and 6 storage servers will run in 6 containers. All containers will run on a single VM.

3.  Client uses our own computer for development.

Entities: Role, state, storage, failure recovery plan
-----------------------------------------------------

1.  Roles: 

1.  Client: 

-   Establishing connection with master.

-   Ask a token from the master.

-   Cache metadata of files.

-   Computing reed-solomon error correction code.

-   Reading from the storage cluster if holding a token given from the master.  

-   Writing to Zookeeper service if holding a valid token. (ZooKeeper service will run above the storage cluster). 

-   Should hold a token (authorized to communicate with storage cluster) and metadata (to perform append write)

-   Merging data chunks if getting a read response from the storage cluster.

1.  Master Service: 

-   Authenticating client to communicate with storage cluster by giving client a token (a token with timeout).

-   Providing metadata of a file to the client if asked by the client.

-   Persisting metadata of every file in the file system.

-   Detecting the failure of storage services.

-   Requesting storage services to perform fault recovery.

1.  Storage Service: 

-   Responsible for storing the actual data, which including:

1.  four Data disks

2.  one Parity disk

3.  one Reed solomon disk

-   Picking the corresponding partition of data if getting a write request from the client.

-   Performing fault recover, which including:

1.  recover current read file.

2.  compute and recover the failed data disks or parity disks.

3.  undo partial write. 

-   Acknowledging master current storage service status by sending heartbeat to master.

1.  Zookeeper: 

-   Responsible for coordinating the write request. 

-   Detecting the failure of storage services.

1.  States:

1.  Normal (Master)

2.  Storage services failure (Master)

3.  Storage services under recovery (Master)

4.  Master failure (Client)

5.  Zookeeper failure 

3.  Failure Recovery Plan:

1.  Master checkpointing 

2.  Storage cluster checkpointing

System Architecture and Interaction
-----------------------------------

-   Client will cache metadata in the home directory once it established connection with master. 

-   If the client doesn't have the desired filePath, it will ask its Meta data from Master.

-   Change directory: ask for MetaData for current directory, upper directory, lower directory

-   The linkedList of the chunk position is the order of chunks. It is used for calculating the server's id.

-   When reading from the storage cluster, it will read from all storage servers and merge the file chunks in the client following the linkedList order.

-   Master has to persist the Metadata of every file.

System Interaction Diagrams of Write and Read Operations 
---------------------------------------------------------

1.  Read(): don't need go through zookeeper 

     Not simultaneously with write ->  later implement with lock

![](https://lh6.googleusercontent.com/FIiLDmFJTlI0lHMx8n7BuaYmDFpDnWwXc8dn-JZ7gdIGIo8MQkvL55yopvUP3stQjyrHekpqxvJQTes70hwksldhzpoL6x-qn-k59tQQeywQDH74tVp3lw95N325RoLlwRGbkTReecGsaLtsAlINezc)

1.  Write() 

replace write, append write, change the ownership(like metadata)

![](https://lh6.googleusercontent.com/7iT6NwcowRoV9KR3KIbkyivXvDgOwHLpw89NNdkD2CIKfmEu9IkcILanjOyCEdSSIU1qPqLUlnDecdUZPfBRK1Q2Bt2Qk-imgatINxeNwDDyWUW4p24ncWJ7nSDhOX9cV7j4gd1JHp825QBKvd0mzA0)

Division of responsibilities among participants
-----------------------------------------------

Hung-Chieh (Storage Cluster): Study Zookeeper or how to split the file to chunks

Chen-Wei (Storage Cluster): Study Storage Services Architecture & Deployment

Hongru Zhou (Reed-Solomon Algorithm): Study How to Implement Reed-Solomon Algorithm and Split files to chunks

Yue Yu (The connection between client, master and storage cluster): Study Spring framework and gRPC 

Library/Framework choices
-------------------------

Client and master servers: Spring Boot framework, gRPC (Communication)

Storage Cluster: Docker (Container deployment), Zookeeper(Storage cluster coordinator), gRPC (Communication)

Algorithm selection

1.  Encoding: reed-solomon and XOR

2.  File split: TODO

Client authentication
---------------------

Tool:  JWT

SDK: <https://jwt.io>

1.  Master: 

-   Produces a token with expiration time.

-   Need to import JWT SDK 

1.  Client: 

-   Obtains the token and use the token as a key to access data in the storage cluster.

1.  Storage Cluster: 

-   Gets the token from the client and determines if the client can be granted access.

-   Compare the current timestamp with the timestamp of the token. 

-   Need to import JWT SDK 

APIs Design
-----------

-   Client Interface: a RSFS CLI: 

1.  Commands need to read file path from storage cluster: cd , tab, ls, rm, mkdir, touch, cat

2.  Commands don't need to read file path from storage cluster: cd .., pwd

-   Client and Master API:

1\. Client ask a token from master (Read() Step 1 and Write() Step 1):

{

String requestType; // "Read" or "Write"

String filePath;

}

2\. Master response to client (Read() Step 2 and Write() Step 2):

Class Node {

Int val; // chunk index

Int id; // storage server id 

Boolean isData;  

}

{

Boolean isHealthy;

String token; // unhealthy: null

List<String> ips; // unhealthy, oath is invalid: null. size == 1 ? Write: Read;

Map<String, LinkedList<Node>> metaData; // {filePath, 0->1->2->3->4->5->6->7} 

Int fileSize; // for locate EoF

}

-   Client and Storage Cluster API:

1.  Client sends a read/write request to servers in the storage cluster. (Read() Step 3 and Write() Step 3)

// Read RPC message (Client read from 6 servers)

{

String operationType; // ls, cd Todo: open file

String path; // If not, null

}

// Write RPC message (Client write to zooKeeper)

   {

String operationType; // rm, mkdir, touch

String path; // original file name

Int fileSize; // so storage cluster can compute the chunk index. If "Delete", old fileSize - new fileSize

Int appendAt; // append index, if it is a create cmd, then the value should be 0

Byte[] payload; // if not, null (moduled by 6 [4data disks + 2 xor/parity disks] )

String writeFlag; // "Replace", "Append", "Delete"

Int lastChunkIdx; // the last chunk index of current file

}

1.  Servers in the storage cluster respond to clients. (Read() Step 4 and Write() Step 6 and Step 9)

// Read RPC response 

{

Byte[] payload; // file chunk;

Boolean tokenIsExpired

Boolean isSuccess;

}

// Write RPC response 

{

Boolean tokenIsExpired;

Boolean isSuccessl;

}

-   Master and Storage Cluster API

1.  Master acknowledges storage cluster to recover (Read Step 5 and Write Step 6)

{

Map<String, LinkedList<Integer>> {fileVersions, 0->1->2->3}; // file version of every file + chunk index

}

1.  Heartbeat (from storage cluster to master) 

{

Map<String, LinkedList<Integer>> {fileVersions, 0->1->2->3}; // file version of every local  file + chunk index

String serverTag; // server id that sends the heartbeat

}

1.  Storage Cluster ack master write success

{\
String fileName;  // original file name

Int AppendAt;

Int fileSize; // so master can compute chunk index

String writeFlag; // "Replace", "Append", different flag will have different linkedlist manipulate strategy

}

1.  Master response to storage cluster when storage cluster update file versions to master in the Write()

{

Boolean isSuccessful;

}
