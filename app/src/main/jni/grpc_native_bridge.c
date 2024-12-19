#include <jni.h>
#include "libgrpc_server.h"
#include <android/log.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <arpa/inet.h>
#include <netinet/in.h>

JNIEXPORT jobject JNICALL
Java_com_example_grpc_client_GrpcNativeBridge_invokeMethod(JNIEnv *env, jobject obj,
                                                           jstring methodName,
                                                           jbyteArray requestBytes,
                                                           jstring callOptionsJson,
                                                           jstring headersJson) {
    // 构建 GrpcRequest 结构体
    GrpcRequest goReq = {0};

    // 获取方法名并转换为 C 字符串
    const char *c_methodName = (*env)->GetStringUTFChars(env, methodName, NULL);
    if (c_methodName == NULL) {
        return NULL; // 出现错误，无法获取方法名
    }
    goReq.method_name = c_methodName;

    // 获取 CallOptions 和 Headers 的 JSON 字符串
    const char *c_options_json = (*env)->GetStringUTFChars(env, callOptionsJson, NULL);
    if (c_options_json == NULL) {
        (*env)->ReleaseStringUTFChars(env, methodName, c_methodName);
        return NULL; // 出现错误，无法获取 CallOptions
    }
    goReq.options_json = c_options_json;

    const char *c_headers_json = (*env)->GetStringUTFChars(env, headersJson, NULL);
    if (c_headers_json == NULL) {
        (*env)->ReleaseStringUTFChars(env, methodName, c_methodName);
        (*env)->ReleaseStringUTFChars(env, callOptionsJson, c_options_json);
        return NULL; // 出现错误，无法获取 Headers
    }
    goReq.headers_json = c_headers_json;

    // 获取请求字节数组及其长度，并转换为 C 字节数组
    jbyte *c_requestBytes = (*env)->GetByteArrayElements(env, requestBytes, NULL);
    if (c_requestBytes == NULL) {
        (*env)->ReleaseStringUTFChars(env, methodName, c_methodName);
        (*env)->ReleaseStringUTFChars(env, callOptionsJson, c_options_json);
        (*env)->ReleaseStringUTFChars(env, headersJson, c_headers_json);
        return NULL; // 出现错误，无法获取请求字节数组
    }
    jsize requestLen = (*env)->GetArrayLength(env, requestBytes);
    goReq.request_len = (size_t) requestLen;
    goReq.request_bytes = (const unsigned char *) c_requestBytes;

    // 准备接收响应数据的 GrpcResponse 结构体
    GrpcResponse goResp = {0};

    // 调用 C 函数
    invokeMethod(goReq, &goResp);

// 清理本地引用
    (*env)->ReleaseStringUTFChars(env, methodName, c_methodName);
    (*env)->ReleaseStringUTFChars(env, callOptionsJson, c_options_json);
    (*env)->ReleaseStringUTFChars(env, headersJson, c_headers_json);
    (*env)->ReleaseByteArrayElements(env, requestBytes, c_requestBytes, JNI_ABORT);

    // 获取 InvokeResult 类
    jclass invokeResultClass = (*env)->FindClass(env, "com/example/grpc/client/InvokeResult");
    if (invokeResultClass == NULL) {
        goto cleanup;
    }

    // 获取 InvokeResult 构造函数
    jmethodID constructor = (*env)->GetMethodID(env, invokeResultClass, "<init>",
                                                "([BILjava/lang/String;Ljava/lang/String;)V");
    if (constructor == NULL) {
        goto cleanup;
    }

    // 检查 response_len 是否在 jsize 的范围内
    if (goResp.response_len > INT_MAX) {
        goto cleanup;
    }
    // 检查 response_len 是否在 jsize 的范围内
    if (goResp.response_len > INT_MAX) {
        return NULL;
    }
    // 显式转换为 jsize
    jsize responseLen = (jsize) goResp.response_len;
    // 创建 Java byte 数组
    jbyteArray byteArray = (*env)->NewByteArray(env, responseLen);
    if (byteArray == NULL) {
        goto cleanup;
    }
    if (responseLen > 0 && goResp.response_bytes != NULL) {
        (*env)->SetByteArrayRegion(env, byteArray, 0, responseLen, (jbyte *) goResp.response_bytes);
    }

    // 创建 InvokeResult 对象
    jobject result = (*env)->NewObject(env, invokeResultClass, constructor,
                                       byteArray,
                                       goResp.status_code, // 错误码
                                       (*env)->NewStringUTF(env, goResp.trailers_json),
                                       (*env)->NewStringUTF(env, goResp.status_message));
    if (result == NULL) {
        goto cleanup;
    }

    // 释放由 Go 分配的内存
    free(goResp.response_bytes);
    free(goResp.status_message);
    free(goResp.trailers_json);

    return result;

    cleanup:
    // 释放由 Go 分配的内存（如果有）
    if (goResp.response_bytes) free(goResp.response_bytes);
    if (goResp.status_message) free(goResp.status_message);
    if (goResp.trailers_json) free(goResp.trailers_json);
    return NULL;
}