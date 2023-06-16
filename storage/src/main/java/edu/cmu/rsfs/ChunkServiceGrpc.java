package edu.cmu.rsfs;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.56.0)",
    comments = "Source: src/main/proto/chunkserver.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class ChunkServiceGrpc {

  private ChunkServiceGrpc() {}

  public static final String SERVICE_NAME = "edu.cmu.rsfs.ChunkService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<edu.cmu.rsfs.Chunkserver.WriteRequest,
      edu.cmu.rsfs.Chunkserver.WriteResponse> getWriteChunkMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "WriteChunk",
      requestType = edu.cmu.rsfs.Chunkserver.WriteRequest.class,
      responseType = edu.cmu.rsfs.Chunkserver.WriteResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<edu.cmu.rsfs.Chunkserver.WriteRequest,
      edu.cmu.rsfs.Chunkserver.WriteResponse> getWriteChunkMethod() {
    io.grpc.MethodDescriptor<edu.cmu.rsfs.Chunkserver.WriteRequest, edu.cmu.rsfs.Chunkserver.WriteResponse> getWriteChunkMethod;
    if ((getWriteChunkMethod = ChunkServiceGrpc.getWriteChunkMethod) == null) {
      synchronized (ChunkServiceGrpc.class) {
        if ((getWriteChunkMethod = ChunkServiceGrpc.getWriteChunkMethod) == null) {
          ChunkServiceGrpc.getWriteChunkMethod = getWriteChunkMethod =
              io.grpc.MethodDescriptor.<edu.cmu.rsfs.Chunkserver.WriteRequest, edu.cmu.rsfs.Chunkserver.WriteResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "WriteChunk"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  edu.cmu.rsfs.Chunkserver.WriteRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  edu.cmu.rsfs.Chunkserver.WriteResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ChunkServiceMethodDescriptorSupplier("WriteChunk"))
              .build();
        }
      }
    }
    return getWriteChunkMethod;
  }

  private static volatile io.grpc.MethodDescriptor<edu.cmu.rsfs.Chunkserver.ReadRequest,
      edu.cmu.rsfs.Chunkserver.ReadResponse> getReadChunkMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ReadChunk",
      requestType = edu.cmu.rsfs.Chunkserver.ReadRequest.class,
      responseType = edu.cmu.rsfs.Chunkserver.ReadResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<edu.cmu.rsfs.Chunkserver.ReadRequest,
      edu.cmu.rsfs.Chunkserver.ReadResponse> getReadChunkMethod() {
    io.grpc.MethodDescriptor<edu.cmu.rsfs.Chunkserver.ReadRequest, edu.cmu.rsfs.Chunkserver.ReadResponse> getReadChunkMethod;
    if ((getReadChunkMethod = ChunkServiceGrpc.getReadChunkMethod) == null) {
      synchronized (ChunkServiceGrpc.class) {
        if ((getReadChunkMethod = ChunkServiceGrpc.getReadChunkMethod) == null) {
          ChunkServiceGrpc.getReadChunkMethod = getReadChunkMethod =
              io.grpc.MethodDescriptor.<edu.cmu.rsfs.Chunkserver.ReadRequest, edu.cmu.rsfs.Chunkserver.ReadResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ReadChunk"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  edu.cmu.rsfs.Chunkserver.ReadRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  edu.cmu.rsfs.Chunkserver.ReadResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ChunkServiceMethodDescriptorSupplier("ReadChunk"))
              .build();
        }
      }
    }
    return getReadChunkMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static ChunkServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ChunkServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ChunkServiceStub>() {
        @java.lang.Override
        public ChunkServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ChunkServiceStub(channel, callOptions);
        }
      };
    return ChunkServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static ChunkServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ChunkServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ChunkServiceBlockingStub>() {
        @java.lang.Override
        public ChunkServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ChunkServiceBlockingStub(channel, callOptions);
        }
      };
    return ChunkServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static ChunkServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ChunkServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ChunkServiceFutureStub>() {
        @java.lang.Override
        public ChunkServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ChunkServiceFutureStub(channel, callOptions);
        }
      };
    return ChunkServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     */
    default void writeChunk(edu.cmu.rsfs.Chunkserver.WriteRequest request,
        io.grpc.stub.StreamObserver<edu.cmu.rsfs.Chunkserver.WriteResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getWriteChunkMethod(), responseObserver);
    }

    /**
     */
    default void readChunk(edu.cmu.rsfs.Chunkserver.ReadRequest request,
        io.grpc.stub.StreamObserver<edu.cmu.rsfs.Chunkserver.ReadResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getReadChunkMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service ChunkService.
   */
  public static abstract class ChunkServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return ChunkServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service ChunkService.
   */
  public static final class ChunkServiceStub
      extends io.grpc.stub.AbstractAsyncStub<ChunkServiceStub> {
    private ChunkServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ChunkServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ChunkServiceStub(channel, callOptions);
    }

    /**
     */
    public void writeChunk(edu.cmu.rsfs.Chunkserver.WriteRequest request,
        io.grpc.stub.StreamObserver<edu.cmu.rsfs.Chunkserver.WriteResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getWriteChunkMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void readChunk(edu.cmu.rsfs.Chunkserver.ReadRequest request,
        io.grpc.stub.StreamObserver<edu.cmu.rsfs.Chunkserver.ReadResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getReadChunkMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service ChunkService.
   */
  public static final class ChunkServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<ChunkServiceBlockingStub> {
    private ChunkServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ChunkServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ChunkServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public edu.cmu.rsfs.Chunkserver.WriteResponse writeChunk(edu.cmu.rsfs.Chunkserver.WriteRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getWriteChunkMethod(), getCallOptions(), request);
    }

    /**
     */
    public edu.cmu.rsfs.Chunkserver.ReadResponse readChunk(edu.cmu.rsfs.Chunkserver.ReadRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getReadChunkMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service ChunkService.
   */
  public static final class ChunkServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<ChunkServiceFutureStub> {
    private ChunkServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ChunkServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ChunkServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<edu.cmu.rsfs.Chunkserver.WriteResponse> writeChunk(
        edu.cmu.rsfs.Chunkserver.WriteRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getWriteChunkMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<edu.cmu.rsfs.Chunkserver.ReadResponse> readChunk(
        edu.cmu.rsfs.Chunkserver.ReadRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getReadChunkMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_WRITE_CHUNK = 0;
  private static final int METHODID_READ_CHUNK = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_WRITE_CHUNK:
          serviceImpl.writeChunk((edu.cmu.rsfs.Chunkserver.WriteRequest) request,
              (io.grpc.stub.StreamObserver<edu.cmu.rsfs.Chunkserver.WriteResponse>) responseObserver);
          break;
        case METHODID_READ_CHUNK:
          serviceImpl.readChunk((edu.cmu.rsfs.Chunkserver.ReadRequest) request,
              (io.grpc.stub.StreamObserver<edu.cmu.rsfs.Chunkserver.ReadResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getWriteChunkMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              edu.cmu.rsfs.Chunkserver.WriteRequest,
              edu.cmu.rsfs.Chunkserver.WriteResponse>(
                service, METHODID_WRITE_CHUNK)))
        .addMethod(
          getReadChunkMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              edu.cmu.rsfs.Chunkserver.ReadRequest,
              edu.cmu.rsfs.Chunkserver.ReadResponse>(
                service, METHODID_READ_CHUNK)))
        .build();
  }

  private static abstract class ChunkServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    ChunkServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return edu.cmu.rsfs.Chunkserver.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("ChunkService");
    }
  }

  private static final class ChunkServiceFileDescriptorSupplier
      extends ChunkServiceBaseDescriptorSupplier {
    ChunkServiceFileDescriptorSupplier() {}
  }

  private static final class ChunkServiceMethodDescriptorSupplier
      extends ChunkServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    ChunkServiceMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (ChunkServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new ChunkServiceFileDescriptorSupplier())
              .addMethod(getWriteChunkMethod())
              .addMethod(getReadChunkMethod())
              .build();
        }
      }
    }
    return result;
  }
}
