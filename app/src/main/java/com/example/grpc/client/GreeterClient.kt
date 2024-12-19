package com.example.grpc.client

import com.example.grpc.pb.greeter.GreeterGrpc
import io.grpc.ManagedChannel

object GreeterClient {
    private val channel: ManagedChannel by lazy {
        NativeManagedChannel(GrpcNativeBridge())
    }
    private val greeterStub: GreeterGrpc.GreeterStub by lazy {
        GreeterGrpc.newStub(channel)
    }

    fun getInstance() = greeterStub
}