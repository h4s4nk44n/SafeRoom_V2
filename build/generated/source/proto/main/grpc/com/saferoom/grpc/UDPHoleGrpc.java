package com.saferoom.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.58.0)",
    comments = "Source: stun.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class UDPHoleGrpc {

  private UDPHoleGrpc() {}

  public static final java.lang.String SERVICE_NAME = "UDPHole";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.saferoom.grpc.SafeRoomProto.Menu,
      com.saferoom.grpc.SafeRoomProto.Status> getMenuAnsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "MenuAns",
      requestType = com.saferoom.grpc.SafeRoomProto.Menu.class,
      responseType = com.saferoom.grpc.SafeRoomProto.Status.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.saferoom.grpc.SafeRoomProto.Menu,
      com.saferoom.grpc.SafeRoomProto.Status> getMenuAnsMethod() {
    io.grpc.MethodDescriptor<com.saferoom.grpc.SafeRoomProto.Menu, com.saferoom.grpc.SafeRoomProto.Status> getMenuAnsMethod;
    if ((getMenuAnsMethod = UDPHoleGrpc.getMenuAnsMethod) == null) {
      synchronized (UDPHoleGrpc.class) {
        if ((getMenuAnsMethod = UDPHoleGrpc.getMenuAnsMethod) == null) {
          UDPHoleGrpc.getMenuAnsMethod = getMenuAnsMethod =
              io.grpc.MethodDescriptor.<com.saferoom.grpc.SafeRoomProto.Menu, com.saferoom.grpc.SafeRoomProto.Status>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "MenuAns"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.saferoom.grpc.SafeRoomProto.Menu.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.saferoom.grpc.SafeRoomProto.Status.getDefaultInstance()))
              .setSchemaDescriptor(new UDPHoleMethodDescriptorSupplier("MenuAns"))
              .build();
        }
      }
    }
    return getMenuAnsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.saferoom.grpc.SafeRoomProto.Create_User,
      com.saferoom.grpc.SafeRoomProto.Status> getInsertUserMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "InsertUser",
      requestType = com.saferoom.grpc.SafeRoomProto.Create_User.class,
      responseType = com.saferoom.grpc.SafeRoomProto.Status.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.saferoom.grpc.SafeRoomProto.Create_User,
      com.saferoom.grpc.SafeRoomProto.Status> getInsertUserMethod() {
    io.grpc.MethodDescriptor<com.saferoom.grpc.SafeRoomProto.Create_User, com.saferoom.grpc.SafeRoomProto.Status> getInsertUserMethod;
    if ((getInsertUserMethod = UDPHoleGrpc.getInsertUserMethod) == null) {
      synchronized (UDPHoleGrpc.class) {
        if ((getInsertUserMethod = UDPHoleGrpc.getInsertUserMethod) == null) {
          UDPHoleGrpc.getInsertUserMethod = getInsertUserMethod =
              io.grpc.MethodDescriptor.<com.saferoom.grpc.SafeRoomProto.Create_User, com.saferoom.grpc.SafeRoomProto.Status>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "InsertUser"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.saferoom.grpc.SafeRoomProto.Create_User.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.saferoom.grpc.SafeRoomProto.Status.getDefaultInstance()))
              .setSchemaDescriptor(new UDPHoleMethodDescriptorSupplier("InsertUser"))
              .build();
        }
      }
    }
    return getInsertUserMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.saferoom.grpc.SafeRoomProto.Verification,
      com.saferoom.grpc.SafeRoomProto.Status> getVerifyUserMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "VerifyUser",
      requestType = com.saferoom.grpc.SafeRoomProto.Verification.class,
      responseType = com.saferoom.grpc.SafeRoomProto.Status.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.saferoom.grpc.SafeRoomProto.Verification,
      com.saferoom.grpc.SafeRoomProto.Status> getVerifyUserMethod() {
    io.grpc.MethodDescriptor<com.saferoom.grpc.SafeRoomProto.Verification, com.saferoom.grpc.SafeRoomProto.Status> getVerifyUserMethod;
    if ((getVerifyUserMethod = UDPHoleGrpc.getVerifyUserMethod) == null) {
      synchronized (UDPHoleGrpc.class) {
        if ((getVerifyUserMethod = UDPHoleGrpc.getVerifyUserMethod) == null) {
          UDPHoleGrpc.getVerifyUserMethod = getVerifyUserMethod =
              io.grpc.MethodDescriptor.<com.saferoom.grpc.SafeRoomProto.Verification, com.saferoom.grpc.SafeRoomProto.Status>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "VerifyUser"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.saferoom.grpc.SafeRoomProto.Verification.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.saferoom.grpc.SafeRoomProto.Status.getDefaultInstance()))
              .setSchemaDescriptor(new UDPHoleMethodDescriptorSupplier("VerifyUser"))
              .build();
        }
      }
    }
    return getVerifyUserMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.saferoom.grpc.SafeRoomProto.Stun_Info,
      com.saferoom.grpc.SafeRoomProto.Status> getRegisterClientMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RegisterClient",
      requestType = com.saferoom.grpc.SafeRoomProto.Stun_Info.class,
      responseType = com.saferoom.grpc.SafeRoomProto.Status.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.saferoom.grpc.SafeRoomProto.Stun_Info,
      com.saferoom.grpc.SafeRoomProto.Status> getRegisterClientMethod() {
    io.grpc.MethodDescriptor<com.saferoom.grpc.SafeRoomProto.Stun_Info, com.saferoom.grpc.SafeRoomProto.Status> getRegisterClientMethod;
    if ((getRegisterClientMethod = UDPHoleGrpc.getRegisterClientMethod) == null) {
      synchronized (UDPHoleGrpc.class) {
        if ((getRegisterClientMethod = UDPHoleGrpc.getRegisterClientMethod) == null) {
          UDPHoleGrpc.getRegisterClientMethod = getRegisterClientMethod =
              io.grpc.MethodDescriptor.<com.saferoom.grpc.SafeRoomProto.Stun_Info, com.saferoom.grpc.SafeRoomProto.Status>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RegisterClient"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.saferoom.grpc.SafeRoomProto.Stun_Info.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.saferoom.grpc.SafeRoomProto.Status.getDefaultInstance()))
              .setSchemaDescriptor(new UDPHoleMethodDescriptorSupplier("RegisterClient"))
              .build();
        }
      }
    }
    return getRegisterClientMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.saferoom.grpc.SafeRoomProto.Request_Client,
      com.saferoom.grpc.SafeRoomProto.Stun_Info> getGetStunInfoMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetStunInfo",
      requestType = com.saferoom.grpc.SafeRoomProto.Request_Client.class,
      responseType = com.saferoom.grpc.SafeRoomProto.Stun_Info.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.saferoom.grpc.SafeRoomProto.Request_Client,
      com.saferoom.grpc.SafeRoomProto.Stun_Info> getGetStunInfoMethod() {
    io.grpc.MethodDescriptor<com.saferoom.grpc.SafeRoomProto.Request_Client, com.saferoom.grpc.SafeRoomProto.Stun_Info> getGetStunInfoMethod;
    if ((getGetStunInfoMethod = UDPHoleGrpc.getGetStunInfoMethod) == null) {
      synchronized (UDPHoleGrpc.class) {
        if ((getGetStunInfoMethod = UDPHoleGrpc.getGetStunInfoMethod) == null) {
          UDPHoleGrpc.getGetStunInfoMethod = getGetStunInfoMethod =
              io.grpc.MethodDescriptor.<com.saferoom.grpc.SafeRoomProto.Request_Client, com.saferoom.grpc.SafeRoomProto.Stun_Info>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetStunInfo"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.saferoom.grpc.SafeRoomProto.Request_Client.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.saferoom.grpc.SafeRoomProto.Stun_Info.getDefaultInstance()))
              .setSchemaDescriptor(new UDPHoleMethodDescriptorSupplier("GetStunInfo"))
              .build();
        }
      }
    }
    return getGetStunInfoMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.saferoom.grpc.SafeRoomProto.FromTo,
      com.saferoom.grpc.SafeRoomProto.Status> getPunchTestMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "PunchTest",
      requestType = com.saferoom.grpc.SafeRoomProto.FromTo.class,
      responseType = com.saferoom.grpc.SafeRoomProto.Status.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.saferoom.grpc.SafeRoomProto.FromTo,
      com.saferoom.grpc.SafeRoomProto.Status> getPunchTestMethod() {
    io.grpc.MethodDescriptor<com.saferoom.grpc.SafeRoomProto.FromTo, com.saferoom.grpc.SafeRoomProto.Status> getPunchTestMethod;
    if ((getPunchTestMethod = UDPHoleGrpc.getPunchTestMethod) == null) {
      synchronized (UDPHoleGrpc.class) {
        if ((getPunchTestMethod = UDPHoleGrpc.getPunchTestMethod) == null) {
          UDPHoleGrpc.getPunchTestMethod = getPunchTestMethod =
              io.grpc.MethodDescriptor.<com.saferoom.grpc.SafeRoomProto.FromTo, com.saferoom.grpc.SafeRoomProto.Status>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "PunchTest"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.saferoom.grpc.SafeRoomProto.FromTo.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.saferoom.grpc.SafeRoomProto.Status.getDefaultInstance()))
              .setSchemaDescriptor(new UDPHoleMethodDescriptorSupplier("PunchTest"))
              .build();
        }
      }
    }
    return getPunchTestMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.saferoom.grpc.SafeRoomProto.HandshakeConfirm,
      com.saferoom.grpc.SafeRoomProto.Status> getHandShakeMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "HandShake",
      requestType = com.saferoom.grpc.SafeRoomProto.HandshakeConfirm.class,
      responseType = com.saferoom.grpc.SafeRoomProto.Status.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.saferoom.grpc.SafeRoomProto.HandshakeConfirm,
      com.saferoom.grpc.SafeRoomProto.Status> getHandShakeMethod() {
    io.grpc.MethodDescriptor<com.saferoom.grpc.SafeRoomProto.HandshakeConfirm, com.saferoom.grpc.SafeRoomProto.Status> getHandShakeMethod;
    if ((getHandShakeMethod = UDPHoleGrpc.getHandShakeMethod) == null) {
      synchronized (UDPHoleGrpc.class) {
        if ((getHandShakeMethod = UDPHoleGrpc.getHandShakeMethod) == null) {
          UDPHoleGrpc.getHandShakeMethod = getHandShakeMethod =
              io.grpc.MethodDescriptor.<com.saferoom.grpc.SafeRoomProto.HandshakeConfirm, com.saferoom.grpc.SafeRoomProto.Status>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "HandShake"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.saferoom.grpc.SafeRoomProto.HandshakeConfirm.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.saferoom.grpc.SafeRoomProto.Status.getDefaultInstance()))
              .setSchemaDescriptor(new UDPHoleMethodDescriptorSupplier("HandShake"))
              .build();
        }
      }
    }
    return getHandShakeMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.saferoom.grpc.SafeRoomProto.Stun_Info,
      com.saferoom.grpc.SafeRoomProto.Status> getHeartBeatMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "HeartBeat",
      requestType = com.saferoom.grpc.SafeRoomProto.Stun_Info.class,
      responseType = com.saferoom.grpc.SafeRoomProto.Status.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.saferoom.grpc.SafeRoomProto.Stun_Info,
      com.saferoom.grpc.SafeRoomProto.Status> getHeartBeatMethod() {
    io.grpc.MethodDescriptor<com.saferoom.grpc.SafeRoomProto.Stun_Info, com.saferoom.grpc.SafeRoomProto.Status> getHeartBeatMethod;
    if ((getHeartBeatMethod = UDPHoleGrpc.getHeartBeatMethod) == null) {
      synchronized (UDPHoleGrpc.class) {
        if ((getHeartBeatMethod = UDPHoleGrpc.getHeartBeatMethod) == null) {
          UDPHoleGrpc.getHeartBeatMethod = getHeartBeatMethod =
              io.grpc.MethodDescriptor.<com.saferoom.grpc.SafeRoomProto.Stun_Info, com.saferoom.grpc.SafeRoomProto.Status>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "HeartBeat"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.saferoom.grpc.SafeRoomProto.Stun_Info.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.saferoom.grpc.SafeRoomProto.Status.getDefaultInstance()))
              .setSchemaDescriptor(new UDPHoleMethodDescriptorSupplier("HeartBeat"))
              .build();
        }
      }
    }
    return getHeartBeatMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.saferoom.grpc.SafeRoomProto.Request_Client,
      com.saferoom.grpc.SafeRoomProto.Status> getFinishMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Finish",
      requestType = com.saferoom.grpc.SafeRoomProto.Request_Client.class,
      responseType = com.saferoom.grpc.SafeRoomProto.Status.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.saferoom.grpc.SafeRoomProto.Request_Client,
      com.saferoom.grpc.SafeRoomProto.Status> getFinishMethod() {
    io.grpc.MethodDescriptor<com.saferoom.grpc.SafeRoomProto.Request_Client, com.saferoom.grpc.SafeRoomProto.Status> getFinishMethod;
    if ((getFinishMethod = UDPHoleGrpc.getFinishMethod) == null) {
      synchronized (UDPHoleGrpc.class) {
        if ((getFinishMethod = UDPHoleGrpc.getFinishMethod) == null) {
          UDPHoleGrpc.getFinishMethod = getFinishMethod =
              io.grpc.MethodDescriptor.<com.saferoom.grpc.SafeRoomProto.Request_Client, com.saferoom.grpc.SafeRoomProto.Status>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Finish"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.saferoom.grpc.SafeRoomProto.Request_Client.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.saferoom.grpc.SafeRoomProto.Status.getDefaultInstance()))
              .setSchemaDescriptor(new UDPHoleMethodDescriptorSupplier("Finish"))
              .build();
        }
      }
    }
    return getFinishMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.saferoom.grpc.SafeRoomProto.Empty,
      com.saferoom.grpc.SafeRoomProto.PublicKeyMessage> getGetServerPublicKeyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetServerPublicKey",
      requestType = com.saferoom.grpc.SafeRoomProto.Empty.class,
      responseType = com.saferoom.grpc.SafeRoomProto.PublicKeyMessage.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.saferoom.grpc.SafeRoomProto.Empty,
      com.saferoom.grpc.SafeRoomProto.PublicKeyMessage> getGetServerPublicKeyMethod() {
    io.grpc.MethodDescriptor<com.saferoom.grpc.SafeRoomProto.Empty, com.saferoom.grpc.SafeRoomProto.PublicKeyMessage> getGetServerPublicKeyMethod;
    if ((getGetServerPublicKeyMethod = UDPHoleGrpc.getGetServerPublicKeyMethod) == null) {
      synchronized (UDPHoleGrpc.class) {
        if ((getGetServerPublicKeyMethod = UDPHoleGrpc.getGetServerPublicKeyMethod) == null) {
          UDPHoleGrpc.getGetServerPublicKeyMethod = getGetServerPublicKeyMethod =
              io.grpc.MethodDescriptor.<com.saferoom.grpc.SafeRoomProto.Empty, com.saferoom.grpc.SafeRoomProto.PublicKeyMessage>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetServerPublicKey"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.saferoom.grpc.SafeRoomProto.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.saferoom.grpc.SafeRoomProto.PublicKeyMessage.getDefaultInstance()))
              .setSchemaDescriptor(new UDPHoleMethodDescriptorSupplier("GetServerPublicKey"))
              .build();
        }
      }
    }
    return getGetServerPublicKeyMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.saferoom.grpc.SafeRoomProto.EncryptedAESKeyMessage,
      com.saferoom.grpc.SafeRoomProto.Status> getSendEncryptedAESKeyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SendEncryptedAESKey",
      requestType = com.saferoom.grpc.SafeRoomProto.EncryptedAESKeyMessage.class,
      responseType = com.saferoom.grpc.SafeRoomProto.Status.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.saferoom.grpc.SafeRoomProto.EncryptedAESKeyMessage,
      com.saferoom.grpc.SafeRoomProto.Status> getSendEncryptedAESKeyMethod() {
    io.grpc.MethodDescriptor<com.saferoom.grpc.SafeRoomProto.EncryptedAESKeyMessage, com.saferoom.grpc.SafeRoomProto.Status> getSendEncryptedAESKeyMethod;
    if ((getSendEncryptedAESKeyMethod = UDPHoleGrpc.getSendEncryptedAESKeyMethod) == null) {
      synchronized (UDPHoleGrpc.class) {
        if ((getSendEncryptedAESKeyMethod = UDPHoleGrpc.getSendEncryptedAESKeyMethod) == null) {
          UDPHoleGrpc.getSendEncryptedAESKeyMethod = getSendEncryptedAESKeyMethod =
              io.grpc.MethodDescriptor.<com.saferoom.grpc.SafeRoomProto.EncryptedAESKeyMessage, com.saferoom.grpc.SafeRoomProto.Status>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SendEncryptedAESKey"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.saferoom.grpc.SafeRoomProto.EncryptedAESKeyMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.saferoom.grpc.SafeRoomProto.Status.getDefaultInstance()))
              .setSchemaDescriptor(new UDPHoleMethodDescriptorSupplier("SendEncryptedAESKey"))
              .build();
        }
      }
    }
    return getSendEncryptedAESKeyMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.saferoom.grpc.SafeRoomProto.RequestByClient_ID,
      com.saferoom.grpc.SafeRoomProto.EncryptedAESKeyMessage> getGetEncryptedAESKeyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetEncryptedAESKey",
      requestType = com.saferoom.grpc.SafeRoomProto.RequestByClient_ID.class,
      responseType = com.saferoom.grpc.SafeRoomProto.EncryptedAESKeyMessage.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.saferoom.grpc.SafeRoomProto.RequestByClient_ID,
      com.saferoom.grpc.SafeRoomProto.EncryptedAESKeyMessage> getGetEncryptedAESKeyMethod() {
    io.grpc.MethodDescriptor<com.saferoom.grpc.SafeRoomProto.RequestByClient_ID, com.saferoom.grpc.SafeRoomProto.EncryptedAESKeyMessage> getGetEncryptedAESKeyMethod;
    if ((getGetEncryptedAESKeyMethod = UDPHoleGrpc.getGetEncryptedAESKeyMethod) == null) {
      synchronized (UDPHoleGrpc.class) {
        if ((getGetEncryptedAESKeyMethod = UDPHoleGrpc.getGetEncryptedAESKeyMethod) == null) {
          UDPHoleGrpc.getGetEncryptedAESKeyMethod = getGetEncryptedAESKeyMethod =
              io.grpc.MethodDescriptor.<com.saferoom.grpc.SafeRoomProto.RequestByClient_ID, com.saferoom.grpc.SafeRoomProto.EncryptedAESKeyMessage>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetEncryptedAESKey"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.saferoom.grpc.SafeRoomProto.RequestByClient_ID.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.saferoom.grpc.SafeRoomProto.EncryptedAESKeyMessage.getDefaultInstance()))
              .setSchemaDescriptor(new UDPHoleMethodDescriptorSupplier("GetEncryptedAESKey"))
              .build();
        }
      }
    }
    return getGetEncryptedAESKeyMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.saferoom.grpc.SafeRoomProto.EncryptedPacket,
      com.saferoom.grpc.SafeRoomProto.Status> getSendEncryptedMessageMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SendEncryptedMessage",
      requestType = com.saferoom.grpc.SafeRoomProto.EncryptedPacket.class,
      responseType = com.saferoom.grpc.SafeRoomProto.Status.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.saferoom.grpc.SafeRoomProto.EncryptedPacket,
      com.saferoom.grpc.SafeRoomProto.Status> getSendEncryptedMessageMethod() {
    io.grpc.MethodDescriptor<com.saferoom.grpc.SafeRoomProto.EncryptedPacket, com.saferoom.grpc.SafeRoomProto.Status> getSendEncryptedMessageMethod;
    if ((getSendEncryptedMessageMethod = UDPHoleGrpc.getSendEncryptedMessageMethod) == null) {
      synchronized (UDPHoleGrpc.class) {
        if ((getSendEncryptedMessageMethod = UDPHoleGrpc.getSendEncryptedMessageMethod) == null) {
          UDPHoleGrpc.getSendEncryptedMessageMethod = getSendEncryptedMessageMethod =
              io.grpc.MethodDescriptor.<com.saferoom.grpc.SafeRoomProto.EncryptedPacket, com.saferoom.grpc.SafeRoomProto.Status>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SendEncryptedMessage"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.saferoom.grpc.SafeRoomProto.EncryptedPacket.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.saferoom.grpc.SafeRoomProto.Status.getDefaultInstance()))
              .setSchemaDescriptor(new UDPHoleMethodDescriptorSupplier("SendEncryptedMessage"))
              .build();
        }
      }
    }
    return getSendEncryptedMessageMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.saferoom.grpc.SafeRoomProto.EncryptedPacket,
      com.saferoom.grpc.SafeRoomProto.DecryptedPacket> getDecryptedMessageMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DecryptedMessage",
      requestType = com.saferoom.grpc.SafeRoomProto.EncryptedPacket.class,
      responseType = com.saferoom.grpc.SafeRoomProto.DecryptedPacket.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.saferoom.grpc.SafeRoomProto.EncryptedPacket,
      com.saferoom.grpc.SafeRoomProto.DecryptedPacket> getDecryptedMessageMethod() {
    io.grpc.MethodDescriptor<com.saferoom.grpc.SafeRoomProto.EncryptedPacket, com.saferoom.grpc.SafeRoomProto.DecryptedPacket> getDecryptedMessageMethod;
    if ((getDecryptedMessageMethod = UDPHoleGrpc.getDecryptedMessageMethod) == null) {
      synchronized (UDPHoleGrpc.class) {
        if ((getDecryptedMessageMethod = UDPHoleGrpc.getDecryptedMessageMethod) == null) {
          UDPHoleGrpc.getDecryptedMessageMethod = getDecryptedMessageMethod =
              io.grpc.MethodDescriptor.<com.saferoom.grpc.SafeRoomProto.EncryptedPacket, com.saferoom.grpc.SafeRoomProto.DecryptedPacket>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DecryptedMessage"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.saferoom.grpc.SafeRoomProto.EncryptedPacket.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.saferoom.grpc.SafeRoomProto.DecryptedPacket.getDefaultInstance()))
              .setSchemaDescriptor(new UDPHoleMethodDescriptorSupplier("DecryptedMessage"))
              .build();
        }
      }
    }
    return getDecryptedMessageMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.saferoom.grpc.SafeRoomProto.SendPublicKeyRequest,
      com.saferoom.grpc.SafeRoomProto.Status> getSendPublicKeyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SendPublicKey",
      requestType = com.saferoom.grpc.SafeRoomProto.SendPublicKeyRequest.class,
      responseType = com.saferoom.grpc.SafeRoomProto.Status.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.saferoom.grpc.SafeRoomProto.SendPublicKeyRequest,
      com.saferoom.grpc.SafeRoomProto.Status> getSendPublicKeyMethod() {
    io.grpc.MethodDescriptor<com.saferoom.grpc.SafeRoomProto.SendPublicKeyRequest, com.saferoom.grpc.SafeRoomProto.Status> getSendPublicKeyMethod;
    if ((getSendPublicKeyMethod = UDPHoleGrpc.getSendPublicKeyMethod) == null) {
      synchronized (UDPHoleGrpc.class) {
        if ((getSendPublicKeyMethod = UDPHoleGrpc.getSendPublicKeyMethod) == null) {
          UDPHoleGrpc.getSendPublicKeyMethod = getSendPublicKeyMethod =
              io.grpc.MethodDescriptor.<com.saferoom.grpc.SafeRoomProto.SendPublicKeyRequest, com.saferoom.grpc.SafeRoomProto.Status>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SendPublicKey"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.saferoom.grpc.SafeRoomProto.SendPublicKeyRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.saferoom.grpc.SafeRoomProto.Status.getDefaultInstance()))
              .setSchemaDescriptor(new UDPHoleMethodDescriptorSupplier("SendPublicKey"))
              .build();
        }
      }
    }
    return getSendPublicKeyMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.saferoom.grpc.SafeRoomProto.RequestByClient_ID,
      com.saferoom.grpc.SafeRoomProto.PublicKeyMessage> getGetPublicKeyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetPublicKey",
      requestType = com.saferoom.grpc.SafeRoomProto.RequestByClient_ID.class,
      responseType = com.saferoom.grpc.SafeRoomProto.PublicKeyMessage.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.saferoom.grpc.SafeRoomProto.RequestByClient_ID,
      com.saferoom.grpc.SafeRoomProto.PublicKeyMessage> getGetPublicKeyMethod() {
    io.grpc.MethodDescriptor<com.saferoom.grpc.SafeRoomProto.RequestByClient_ID, com.saferoom.grpc.SafeRoomProto.PublicKeyMessage> getGetPublicKeyMethod;
    if ((getGetPublicKeyMethod = UDPHoleGrpc.getGetPublicKeyMethod) == null) {
      synchronized (UDPHoleGrpc.class) {
        if ((getGetPublicKeyMethod = UDPHoleGrpc.getGetPublicKeyMethod) == null) {
          UDPHoleGrpc.getGetPublicKeyMethod = getGetPublicKeyMethod =
              io.grpc.MethodDescriptor.<com.saferoom.grpc.SafeRoomProto.RequestByClient_ID, com.saferoom.grpc.SafeRoomProto.PublicKeyMessage>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetPublicKey"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.saferoom.grpc.SafeRoomProto.RequestByClient_ID.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.saferoom.grpc.SafeRoomProto.PublicKeyMessage.getDefaultInstance()))
              .setSchemaDescriptor(new UDPHoleMethodDescriptorSupplier("GetPublicKey"))
              .build();
        }
      }
    }
    return getGetPublicKeyMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static UDPHoleStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<UDPHoleStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<UDPHoleStub>() {
        @java.lang.Override
        public UDPHoleStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new UDPHoleStub(channel, callOptions);
        }
      };
    return UDPHoleStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static UDPHoleBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<UDPHoleBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<UDPHoleBlockingStub>() {
        @java.lang.Override
        public UDPHoleBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new UDPHoleBlockingStub(channel, callOptions);
        }
      };
    return UDPHoleBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static UDPHoleFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<UDPHoleFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<UDPHoleFutureStub>() {
        @java.lang.Override
        public UDPHoleFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new UDPHoleFutureStub(channel, callOptions);
        }
      };
    return UDPHoleFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     */
    default void menuAns(com.saferoom.grpc.SafeRoomProto.Menu request,
        io.grpc.stub.StreamObserver<com.saferoom.grpc.SafeRoomProto.Status> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getMenuAnsMethod(), responseObserver);
    }

    /**
     */
    default void insertUser(com.saferoom.grpc.SafeRoomProto.Create_User request,
        io.grpc.stub.StreamObserver<com.saferoom.grpc.SafeRoomProto.Status> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getInsertUserMethod(), responseObserver);
    }

    /**
     */
    default void verifyUser(com.saferoom.grpc.SafeRoomProto.Verification request,
        io.grpc.stub.StreamObserver<com.saferoom.grpc.SafeRoomProto.Status> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getVerifyUserMethod(), responseObserver);
    }

    /**
     */
    default void registerClient(com.saferoom.grpc.SafeRoomProto.Stun_Info request,
        io.grpc.stub.StreamObserver<com.saferoom.grpc.SafeRoomProto.Status> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRegisterClientMethod(), responseObserver);
    }

    /**
     */
    default void getStunInfo(com.saferoom.grpc.SafeRoomProto.Request_Client request,
        io.grpc.stub.StreamObserver<com.saferoom.grpc.SafeRoomProto.Stun_Info> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetStunInfoMethod(), responseObserver);
    }

    /**
     */
    default void punchTest(com.saferoom.grpc.SafeRoomProto.FromTo request,
        io.grpc.stub.StreamObserver<com.saferoom.grpc.SafeRoomProto.Status> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getPunchTestMethod(), responseObserver);
    }

    /**
     */
    default void handShake(com.saferoom.grpc.SafeRoomProto.HandshakeConfirm request,
        io.grpc.stub.StreamObserver<com.saferoom.grpc.SafeRoomProto.Status> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getHandShakeMethod(), responseObserver);
    }

    /**
     */
    default void heartBeat(com.saferoom.grpc.SafeRoomProto.Stun_Info request,
        io.grpc.stub.StreamObserver<com.saferoom.grpc.SafeRoomProto.Status> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getHeartBeatMethod(), responseObserver);
    }

    /**
     */
    default void finish(com.saferoom.grpc.SafeRoomProto.Request_Client request,
        io.grpc.stub.StreamObserver<com.saferoom.grpc.SafeRoomProto.Status> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getFinishMethod(), responseObserver);
    }

    /**
     */
    default void getServerPublicKey(com.saferoom.grpc.SafeRoomProto.Empty request,
        io.grpc.stub.StreamObserver<com.saferoom.grpc.SafeRoomProto.PublicKeyMessage> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetServerPublicKeyMethod(), responseObserver);
    }

    /**
     */
    default void sendEncryptedAESKey(com.saferoom.grpc.SafeRoomProto.EncryptedAESKeyMessage request,
        io.grpc.stub.StreamObserver<com.saferoom.grpc.SafeRoomProto.Status> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSendEncryptedAESKeyMethod(), responseObserver);
    }

    /**
     */
    default void getEncryptedAESKey(com.saferoom.grpc.SafeRoomProto.RequestByClient_ID request,
        io.grpc.stub.StreamObserver<com.saferoom.grpc.SafeRoomProto.EncryptedAESKeyMessage> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetEncryptedAESKeyMethod(), responseObserver);
    }

    /**
     */
    default void sendEncryptedMessage(com.saferoom.grpc.SafeRoomProto.EncryptedPacket request,
        io.grpc.stub.StreamObserver<com.saferoom.grpc.SafeRoomProto.Status> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSendEncryptedMessageMethod(), responseObserver);
    }

    /**
     */
    default void decryptedMessage(com.saferoom.grpc.SafeRoomProto.EncryptedPacket request,
        io.grpc.stub.StreamObserver<com.saferoom.grpc.SafeRoomProto.DecryptedPacket> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDecryptedMessageMethod(), responseObserver);
    }

    /**
     */
    default void sendPublicKey(com.saferoom.grpc.SafeRoomProto.SendPublicKeyRequest request,
        io.grpc.stub.StreamObserver<com.saferoom.grpc.SafeRoomProto.Status> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSendPublicKeyMethod(), responseObserver);
    }

    /**
     */
    default void getPublicKey(com.saferoom.grpc.SafeRoomProto.RequestByClient_ID request,
        io.grpc.stub.StreamObserver<com.saferoom.grpc.SafeRoomProto.PublicKeyMessage> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetPublicKeyMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service UDPHole.
   */
  public static abstract class UDPHoleImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return UDPHoleGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service UDPHole.
   */
  public static final class UDPHoleStub
      extends io.grpc.stub.AbstractAsyncStub<UDPHoleStub> {
    private UDPHoleStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected UDPHoleStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new UDPHoleStub(channel, callOptions);
    }

    /**
     */
    public void menuAns(com.saferoom.grpc.SafeRoomProto.Menu request,
        io.grpc.stub.StreamObserver<com.saferoom.grpc.SafeRoomProto.Status> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getMenuAnsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void insertUser(com.saferoom.grpc.SafeRoomProto.Create_User request,
        io.grpc.stub.StreamObserver<com.saferoom.grpc.SafeRoomProto.Status> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getInsertUserMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void verifyUser(com.saferoom.grpc.SafeRoomProto.Verification request,
        io.grpc.stub.StreamObserver<com.saferoom.grpc.SafeRoomProto.Status> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getVerifyUserMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void registerClient(com.saferoom.grpc.SafeRoomProto.Stun_Info request,
        io.grpc.stub.StreamObserver<com.saferoom.grpc.SafeRoomProto.Status> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRegisterClientMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getStunInfo(com.saferoom.grpc.SafeRoomProto.Request_Client request,
        io.grpc.stub.StreamObserver<com.saferoom.grpc.SafeRoomProto.Stun_Info> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetStunInfoMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void punchTest(com.saferoom.grpc.SafeRoomProto.FromTo request,
        io.grpc.stub.StreamObserver<com.saferoom.grpc.SafeRoomProto.Status> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getPunchTestMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void handShake(com.saferoom.grpc.SafeRoomProto.HandshakeConfirm request,
        io.grpc.stub.StreamObserver<com.saferoom.grpc.SafeRoomProto.Status> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getHandShakeMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void heartBeat(com.saferoom.grpc.SafeRoomProto.Stun_Info request,
        io.grpc.stub.StreamObserver<com.saferoom.grpc.SafeRoomProto.Status> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getHeartBeatMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void finish(com.saferoom.grpc.SafeRoomProto.Request_Client request,
        io.grpc.stub.StreamObserver<com.saferoom.grpc.SafeRoomProto.Status> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getFinishMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getServerPublicKey(com.saferoom.grpc.SafeRoomProto.Empty request,
        io.grpc.stub.StreamObserver<com.saferoom.grpc.SafeRoomProto.PublicKeyMessage> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetServerPublicKeyMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void sendEncryptedAESKey(com.saferoom.grpc.SafeRoomProto.EncryptedAESKeyMessage request,
        io.grpc.stub.StreamObserver<com.saferoom.grpc.SafeRoomProto.Status> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSendEncryptedAESKeyMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getEncryptedAESKey(com.saferoom.grpc.SafeRoomProto.RequestByClient_ID request,
        io.grpc.stub.StreamObserver<com.saferoom.grpc.SafeRoomProto.EncryptedAESKeyMessage> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetEncryptedAESKeyMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void sendEncryptedMessage(com.saferoom.grpc.SafeRoomProto.EncryptedPacket request,
        io.grpc.stub.StreamObserver<com.saferoom.grpc.SafeRoomProto.Status> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSendEncryptedMessageMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void decryptedMessage(com.saferoom.grpc.SafeRoomProto.EncryptedPacket request,
        io.grpc.stub.StreamObserver<com.saferoom.grpc.SafeRoomProto.DecryptedPacket> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDecryptedMessageMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void sendPublicKey(com.saferoom.grpc.SafeRoomProto.SendPublicKeyRequest request,
        io.grpc.stub.StreamObserver<com.saferoom.grpc.SafeRoomProto.Status> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSendPublicKeyMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getPublicKey(com.saferoom.grpc.SafeRoomProto.RequestByClient_ID request,
        io.grpc.stub.StreamObserver<com.saferoom.grpc.SafeRoomProto.PublicKeyMessage> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetPublicKeyMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service UDPHole.
   */
  public static final class UDPHoleBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<UDPHoleBlockingStub> {
    private UDPHoleBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected UDPHoleBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new UDPHoleBlockingStub(channel, callOptions);
    }

    /**
     */
    public com.saferoom.grpc.SafeRoomProto.Status menuAns(com.saferoom.grpc.SafeRoomProto.Menu request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getMenuAnsMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.saferoom.grpc.SafeRoomProto.Status insertUser(com.saferoom.grpc.SafeRoomProto.Create_User request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getInsertUserMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.saferoom.grpc.SafeRoomProto.Status verifyUser(com.saferoom.grpc.SafeRoomProto.Verification request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getVerifyUserMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.saferoom.grpc.SafeRoomProto.Status registerClient(com.saferoom.grpc.SafeRoomProto.Stun_Info request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRegisterClientMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.saferoom.grpc.SafeRoomProto.Stun_Info getStunInfo(com.saferoom.grpc.SafeRoomProto.Request_Client request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetStunInfoMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.saferoom.grpc.SafeRoomProto.Status punchTest(com.saferoom.grpc.SafeRoomProto.FromTo request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getPunchTestMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.saferoom.grpc.SafeRoomProto.Status handShake(com.saferoom.grpc.SafeRoomProto.HandshakeConfirm request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getHandShakeMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.saferoom.grpc.SafeRoomProto.Status heartBeat(com.saferoom.grpc.SafeRoomProto.Stun_Info request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getHeartBeatMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.saferoom.grpc.SafeRoomProto.Status finish(com.saferoom.grpc.SafeRoomProto.Request_Client request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getFinishMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.saferoom.grpc.SafeRoomProto.PublicKeyMessage getServerPublicKey(com.saferoom.grpc.SafeRoomProto.Empty request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetServerPublicKeyMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.saferoom.grpc.SafeRoomProto.Status sendEncryptedAESKey(com.saferoom.grpc.SafeRoomProto.EncryptedAESKeyMessage request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSendEncryptedAESKeyMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.saferoom.grpc.SafeRoomProto.EncryptedAESKeyMessage getEncryptedAESKey(com.saferoom.grpc.SafeRoomProto.RequestByClient_ID request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetEncryptedAESKeyMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.saferoom.grpc.SafeRoomProto.Status sendEncryptedMessage(com.saferoom.grpc.SafeRoomProto.EncryptedPacket request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSendEncryptedMessageMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.saferoom.grpc.SafeRoomProto.DecryptedPacket decryptedMessage(com.saferoom.grpc.SafeRoomProto.EncryptedPacket request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDecryptedMessageMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.saferoom.grpc.SafeRoomProto.Status sendPublicKey(com.saferoom.grpc.SafeRoomProto.SendPublicKeyRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSendPublicKeyMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.saferoom.grpc.SafeRoomProto.PublicKeyMessage getPublicKey(com.saferoom.grpc.SafeRoomProto.RequestByClient_ID request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetPublicKeyMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service UDPHole.
   */
  public static final class UDPHoleFutureStub
      extends io.grpc.stub.AbstractFutureStub<UDPHoleFutureStub> {
    private UDPHoleFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected UDPHoleFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new UDPHoleFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.saferoom.grpc.SafeRoomProto.Status> menuAns(
        com.saferoom.grpc.SafeRoomProto.Menu request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getMenuAnsMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.saferoom.grpc.SafeRoomProto.Status> insertUser(
        com.saferoom.grpc.SafeRoomProto.Create_User request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getInsertUserMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.saferoom.grpc.SafeRoomProto.Status> verifyUser(
        com.saferoom.grpc.SafeRoomProto.Verification request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getVerifyUserMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.saferoom.grpc.SafeRoomProto.Status> registerClient(
        com.saferoom.grpc.SafeRoomProto.Stun_Info request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRegisterClientMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.saferoom.grpc.SafeRoomProto.Stun_Info> getStunInfo(
        com.saferoom.grpc.SafeRoomProto.Request_Client request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetStunInfoMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.saferoom.grpc.SafeRoomProto.Status> punchTest(
        com.saferoom.grpc.SafeRoomProto.FromTo request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getPunchTestMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.saferoom.grpc.SafeRoomProto.Status> handShake(
        com.saferoom.grpc.SafeRoomProto.HandshakeConfirm request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getHandShakeMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.saferoom.grpc.SafeRoomProto.Status> heartBeat(
        com.saferoom.grpc.SafeRoomProto.Stun_Info request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getHeartBeatMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.saferoom.grpc.SafeRoomProto.Status> finish(
        com.saferoom.grpc.SafeRoomProto.Request_Client request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getFinishMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.saferoom.grpc.SafeRoomProto.PublicKeyMessage> getServerPublicKey(
        com.saferoom.grpc.SafeRoomProto.Empty request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetServerPublicKeyMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.saferoom.grpc.SafeRoomProto.Status> sendEncryptedAESKey(
        com.saferoom.grpc.SafeRoomProto.EncryptedAESKeyMessage request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSendEncryptedAESKeyMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.saferoom.grpc.SafeRoomProto.EncryptedAESKeyMessage> getEncryptedAESKey(
        com.saferoom.grpc.SafeRoomProto.RequestByClient_ID request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetEncryptedAESKeyMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.saferoom.grpc.SafeRoomProto.Status> sendEncryptedMessage(
        com.saferoom.grpc.SafeRoomProto.EncryptedPacket request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSendEncryptedMessageMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.saferoom.grpc.SafeRoomProto.DecryptedPacket> decryptedMessage(
        com.saferoom.grpc.SafeRoomProto.EncryptedPacket request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDecryptedMessageMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.saferoom.grpc.SafeRoomProto.Status> sendPublicKey(
        com.saferoom.grpc.SafeRoomProto.SendPublicKeyRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSendPublicKeyMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.saferoom.grpc.SafeRoomProto.PublicKeyMessage> getPublicKey(
        com.saferoom.grpc.SafeRoomProto.RequestByClient_ID request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetPublicKeyMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_MENU_ANS = 0;
  private static final int METHODID_INSERT_USER = 1;
  private static final int METHODID_VERIFY_USER = 2;
  private static final int METHODID_REGISTER_CLIENT = 3;
  private static final int METHODID_GET_STUN_INFO = 4;
  private static final int METHODID_PUNCH_TEST = 5;
  private static final int METHODID_HAND_SHAKE = 6;
  private static final int METHODID_HEART_BEAT = 7;
  private static final int METHODID_FINISH = 8;
  private static final int METHODID_GET_SERVER_PUBLIC_KEY = 9;
  private static final int METHODID_SEND_ENCRYPTED_AESKEY = 10;
  private static final int METHODID_GET_ENCRYPTED_AESKEY = 11;
  private static final int METHODID_SEND_ENCRYPTED_MESSAGE = 12;
  private static final int METHODID_DECRYPTED_MESSAGE = 13;
  private static final int METHODID_SEND_PUBLIC_KEY = 14;
  private static final int METHODID_GET_PUBLIC_KEY = 15;

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
        case METHODID_MENU_ANS:
          serviceImpl.menuAns((com.saferoom.grpc.SafeRoomProto.Menu) request,
              (io.grpc.stub.StreamObserver<com.saferoom.grpc.SafeRoomProto.Status>) responseObserver);
          break;
        case METHODID_INSERT_USER:
          serviceImpl.insertUser((com.saferoom.grpc.SafeRoomProto.Create_User) request,
              (io.grpc.stub.StreamObserver<com.saferoom.grpc.SafeRoomProto.Status>) responseObserver);
          break;
        case METHODID_VERIFY_USER:
          serviceImpl.verifyUser((com.saferoom.grpc.SafeRoomProto.Verification) request,
              (io.grpc.stub.StreamObserver<com.saferoom.grpc.SafeRoomProto.Status>) responseObserver);
          break;
        case METHODID_REGISTER_CLIENT:
          serviceImpl.registerClient((com.saferoom.grpc.SafeRoomProto.Stun_Info) request,
              (io.grpc.stub.StreamObserver<com.saferoom.grpc.SafeRoomProto.Status>) responseObserver);
          break;
        case METHODID_GET_STUN_INFO:
          serviceImpl.getStunInfo((com.saferoom.grpc.SafeRoomProto.Request_Client) request,
              (io.grpc.stub.StreamObserver<com.saferoom.grpc.SafeRoomProto.Stun_Info>) responseObserver);
          break;
        case METHODID_PUNCH_TEST:
          serviceImpl.punchTest((com.saferoom.grpc.SafeRoomProto.FromTo) request,
              (io.grpc.stub.StreamObserver<com.saferoom.grpc.SafeRoomProto.Status>) responseObserver);
          break;
        case METHODID_HAND_SHAKE:
          serviceImpl.handShake((com.saferoom.grpc.SafeRoomProto.HandshakeConfirm) request,
              (io.grpc.stub.StreamObserver<com.saferoom.grpc.SafeRoomProto.Status>) responseObserver);
          break;
        case METHODID_HEART_BEAT:
          serviceImpl.heartBeat((com.saferoom.grpc.SafeRoomProto.Stun_Info) request,
              (io.grpc.stub.StreamObserver<com.saferoom.grpc.SafeRoomProto.Status>) responseObserver);
          break;
        case METHODID_FINISH:
          serviceImpl.finish((com.saferoom.grpc.SafeRoomProto.Request_Client) request,
              (io.grpc.stub.StreamObserver<com.saferoom.grpc.SafeRoomProto.Status>) responseObserver);
          break;
        case METHODID_GET_SERVER_PUBLIC_KEY:
          serviceImpl.getServerPublicKey((com.saferoom.grpc.SafeRoomProto.Empty) request,
              (io.grpc.stub.StreamObserver<com.saferoom.grpc.SafeRoomProto.PublicKeyMessage>) responseObserver);
          break;
        case METHODID_SEND_ENCRYPTED_AESKEY:
          serviceImpl.sendEncryptedAESKey((com.saferoom.grpc.SafeRoomProto.EncryptedAESKeyMessage) request,
              (io.grpc.stub.StreamObserver<com.saferoom.grpc.SafeRoomProto.Status>) responseObserver);
          break;
        case METHODID_GET_ENCRYPTED_AESKEY:
          serviceImpl.getEncryptedAESKey((com.saferoom.grpc.SafeRoomProto.RequestByClient_ID) request,
              (io.grpc.stub.StreamObserver<com.saferoom.grpc.SafeRoomProto.EncryptedAESKeyMessage>) responseObserver);
          break;
        case METHODID_SEND_ENCRYPTED_MESSAGE:
          serviceImpl.sendEncryptedMessage((com.saferoom.grpc.SafeRoomProto.EncryptedPacket) request,
              (io.grpc.stub.StreamObserver<com.saferoom.grpc.SafeRoomProto.Status>) responseObserver);
          break;
        case METHODID_DECRYPTED_MESSAGE:
          serviceImpl.decryptedMessage((com.saferoom.grpc.SafeRoomProto.EncryptedPacket) request,
              (io.grpc.stub.StreamObserver<com.saferoom.grpc.SafeRoomProto.DecryptedPacket>) responseObserver);
          break;
        case METHODID_SEND_PUBLIC_KEY:
          serviceImpl.sendPublicKey((com.saferoom.grpc.SafeRoomProto.SendPublicKeyRequest) request,
              (io.grpc.stub.StreamObserver<com.saferoom.grpc.SafeRoomProto.Status>) responseObserver);
          break;
        case METHODID_GET_PUBLIC_KEY:
          serviceImpl.getPublicKey((com.saferoom.grpc.SafeRoomProto.RequestByClient_ID) request,
              (io.grpc.stub.StreamObserver<com.saferoom.grpc.SafeRoomProto.PublicKeyMessage>) responseObserver);
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
          getMenuAnsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.saferoom.grpc.SafeRoomProto.Menu,
              com.saferoom.grpc.SafeRoomProto.Status>(
                service, METHODID_MENU_ANS)))
        .addMethod(
          getInsertUserMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.saferoom.grpc.SafeRoomProto.Create_User,
              com.saferoom.grpc.SafeRoomProto.Status>(
                service, METHODID_INSERT_USER)))
        .addMethod(
          getVerifyUserMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.saferoom.grpc.SafeRoomProto.Verification,
              com.saferoom.grpc.SafeRoomProto.Status>(
                service, METHODID_VERIFY_USER)))
        .addMethod(
          getRegisterClientMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.saferoom.grpc.SafeRoomProto.Stun_Info,
              com.saferoom.grpc.SafeRoomProto.Status>(
                service, METHODID_REGISTER_CLIENT)))
        .addMethod(
          getGetStunInfoMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.saferoom.grpc.SafeRoomProto.Request_Client,
              com.saferoom.grpc.SafeRoomProto.Stun_Info>(
                service, METHODID_GET_STUN_INFO)))
        .addMethod(
          getPunchTestMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.saferoom.grpc.SafeRoomProto.FromTo,
              com.saferoom.grpc.SafeRoomProto.Status>(
                service, METHODID_PUNCH_TEST)))
        .addMethod(
          getHandShakeMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.saferoom.grpc.SafeRoomProto.HandshakeConfirm,
              com.saferoom.grpc.SafeRoomProto.Status>(
                service, METHODID_HAND_SHAKE)))
        .addMethod(
          getHeartBeatMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.saferoom.grpc.SafeRoomProto.Stun_Info,
              com.saferoom.grpc.SafeRoomProto.Status>(
                service, METHODID_HEART_BEAT)))
        .addMethod(
          getFinishMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.saferoom.grpc.SafeRoomProto.Request_Client,
              com.saferoom.grpc.SafeRoomProto.Status>(
                service, METHODID_FINISH)))
        .addMethod(
          getGetServerPublicKeyMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.saferoom.grpc.SafeRoomProto.Empty,
              com.saferoom.grpc.SafeRoomProto.PublicKeyMessage>(
                service, METHODID_GET_SERVER_PUBLIC_KEY)))
        .addMethod(
          getSendEncryptedAESKeyMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.saferoom.grpc.SafeRoomProto.EncryptedAESKeyMessage,
              com.saferoom.grpc.SafeRoomProto.Status>(
                service, METHODID_SEND_ENCRYPTED_AESKEY)))
        .addMethod(
          getGetEncryptedAESKeyMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.saferoom.grpc.SafeRoomProto.RequestByClient_ID,
              com.saferoom.grpc.SafeRoomProto.EncryptedAESKeyMessage>(
                service, METHODID_GET_ENCRYPTED_AESKEY)))
        .addMethod(
          getSendEncryptedMessageMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.saferoom.grpc.SafeRoomProto.EncryptedPacket,
              com.saferoom.grpc.SafeRoomProto.Status>(
                service, METHODID_SEND_ENCRYPTED_MESSAGE)))
        .addMethod(
          getDecryptedMessageMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.saferoom.grpc.SafeRoomProto.EncryptedPacket,
              com.saferoom.grpc.SafeRoomProto.DecryptedPacket>(
                service, METHODID_DECRYPTED_MESSAGE)))
        .addMethod(
          getSendPublicKeyMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.saferoom.grpc.SafeRoomProto.SendPublicKeyRequest,
              com.saferoom.grpc.SafeRoomProto.Status>(
                service, METHODID_SEND_PUBLIC_KEY)))
        .addMethod(
          getGetPublicKeyMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.saferoom.grpc.SafeRoomProto.RequestByClient_ID,
              com.saferoom.grpc.SafeRoomProto.PublicKeyMessage>(
                service, METHODID_GET_PUBLIC_KEY)))
        .build();
  }

  private static abstract class UDPHoleBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    UDPHoleBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.saferoom.grpc.SafeRoomProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("UDPHole");
    }
  }

  private static final class UDPHoleFileDescriptorSupplier
      extends UDPHoleBaseDescriptorSupplier {
    UDPHoleFileDescriptorSupplier() {}
  }

  private static final class UDPHoleMethodDescriptorSupplier
      extends UDPHoleBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    UDPHoleMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (UDPHoleGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new UDPHoleFileDescriptorSupplier())
              .addMethod(getMenuAnsMethod())
              .addMethod(getInsertUserMethod())
              .addMethod(getVerifyUserMethod())
              .addMethod(getRegisterClientMethod())
              .addMethod(getGetStunInfoMethod())
              .addMethod(getPunchTestMethod())
              .addMethod(getHandShakeMethod())
              .addMethod(getHeartBeatMethod())
              .addMethod(getFinishMethod())
              .addMethod(getGetServerPublicKeyMethod())
              .addMethod(getSendEncryptedAESKeyMethod())
              .addMethod(getGetEncryptedAESKeyMethod())
              .addMethod(getSendEncryptedMessageMethod())
              .addMethod(getDecryptedMessageMethod())
              .addMethod(getSendPublicKeyMethod())
              .addMethod(getGetPublicKeyMethod())
              .build();
        }
      }
    }
    return result;
  }
}
