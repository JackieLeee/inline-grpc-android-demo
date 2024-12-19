package com.example.grpc.client

/**
 * GrpcNativeBridge Java代码与本地gRPC服务之间的桥梁
 */
class GrpcNativeBridge {
    // 加载JNI库
    init {
        System.loadLibrary("grpc_native_bridge")
    }

    /**
     * 调用本地方法
     * @param methodName 方法名
     * @param requestBytes 输入参数(经过proto序列化之后的字节数组)
     * @param callOptionsJson 调用选项，包括元数据等。
     * @param headersJson 请求头
     */
    external fun invokeMethod(
        methodName: String,
        requestBytes: ByteArray,
        callOptionsJson: String,
        headersJson: String
    ): InvokeResult
}

/**
 * 表示 gRPC 方法调用的结果。
 *
 * @property responseBytes 响应数据，作为字节数组。
 * @property status 调用的状态，包括状态码和描述。
 * @property trailers 响应中的拖车（trailer），即额外的元数据。
 * @property error 可能发生的异常或错误信息。
 */
data class InvokeResult(
    val responseBytes: ByteArray?,
    val status: Int,
    val trailers: String?,
    val error: String?
)