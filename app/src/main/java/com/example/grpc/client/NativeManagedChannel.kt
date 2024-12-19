package com.example.grpc.client

import com.google.protobuf.Message
import io.grpc.CallOptions
import io.grpc.ClientCall
import io.grpc.ManagedChannel
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.Status
import io.grpc.StatusRuntimeException
import java.io.ByteArrayInputStream
import java.util.concurrent.TimeUnit

/**
 * NativeManagedChannel 自定义的ManagedChannel实现，接管替代grpc原有的HTTP/2通信方式
 */
class NativeManagedChannel(private val grpcNativeBridge: GrpcNativeBridge) : ManagedChannel() {
    override fun shutdown(): ManagedChannel? = null

    override fun isShutdown(): Boolean = false

    override fun isTerminated(): Boolean = false

    override fun shutdownNow(): ManagedChannel? = null

    override fun awaitTermination(timeout: Long, unit: TimeUnit?): Boolean = false

    // 创建一个新的 gRPC 调用
    override fun <RequestT : Any, ResponseT : Any> newCall(
        methodDescriptor: MethodDescriptor<RequestT, ResponseT>,
        callOptions: CallOptions
    ): ClientCall<RequestT, ResponseT> {
        return object : ClientCall<RequestT, ResponseT>() {
            private lateinit var responseObserver: Listener<ResponseT>
            private lateinit var headers: Metadata

            // 开始调用时，设置响应监听器
            override fun start(responseListener: Listener<ResponseT>, headers: Metadata?) {
                this.responseObserver = responseListener
                this.headers = headers ?: Metadata()
            }

            // 发送消息到grpc服务
            override fun sendMessage(message: RequestT) {
                try {
                    // 从方法描述符中获取方法名称
                    val bareMethodName = methodDescriptor.bareMethodName
                        ?: throw IllegalArgumentException("Method name must be specified and non-empty")
                    // 将请求消息序列化为字节数组
                    val input = serializeRequest(message)
                    // 调用本地 gRPC方法
                    val result = grpcNativeBridge.invokeMethod(bareMethodName, input, this.headers.toString(), callOptions.toString())
                    // 如果调用结果中包含错误信息，则抛出异常
                    if (!result.error.isNullOrEmpty()) {
                        throw StatusRuntimeException(Status.INTERNAL.withDescription(result.error))
                    }
                    if (result.status != 0) {
                        // 如果调用结果状态不为0，则抛出异常
                        throw StatusRuntimeException(Status.fromCodeValue(result.status).withDescription(result.error))
                    }
                    // 将响应结果反序列化，并传递给响应观察者
                    val response = deserializeResponse(result.responseBytes, methodDescriptor)
                    responseObserver.onMessage(response)
                    // 处理元数据
                    val metadata = Metadata()
                    if (result.trailers != null) {
                        metadata.put(Metadata.Key.of("trailers", Metadata.ASCII_STRING_MARSHALLER), result.trailers)
                    }
                    responseObserver.onClose(Status.OK, metadata)
                } catch (e: Exception) {
                    // 如果发生错误，使用 INTERNAL 状态关闭调用
                    responseObserver.onClose(
                        Status.INTERNAL.withDescription(e.message),
                        Metadata()
                    )
                }
            }

            override fun request(numMessages: Int) {}

            override fun cancel(message: String?, cause: Throwable?) {}

            override fun halfClose() {}

            // 将字节数组反序列化为响应对象
            private fun serializeRequest(message: RequestT): ByteArray {
                if (message is Message) {
                    return message.toByteArray()
                }
                throw IllegalArgumentException("Request must be a protobuf message")
            }

            // 将字节数组反序列化为响应对象
            private fun deserializeResponse(
                output: ByteArray?,
                methodDescriptor: MethodDescriptor<*, ResponseT>
            ): ResponseT {
                ByteArrayInputStream(output).use { inputStream ->
                    return methodDescriptor.responseMarshaller.parse(inputStream)
                }
            }
        }
    }

    override fun authority(): String? = null
}