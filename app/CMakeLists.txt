cmake_minimum_required(VERSION 3.10.2)

project(android_lib)

find_library(
        std-lib
        android
)

message("CPU ABI: ${CMAKE_ANDROID_ARCH_ABI}")

# 复制头文件
file(COPY ${CMAKE_CURRENT_SOURCE_DIR}/jniLibs/${CMAKE_ANDROID_ARCH_ABI}/libgrpc_server.h DESTINATION ${CMAKE_CURRENT_SOURCE_DIR}/src/main/jni)
# 扫描jni目录下的头文件和库文件, 并设置给JNI_SRC
file(GLOB JNI_SRC ${CMAKE_CURRENT_SOURCE_DIR}/src/main/jni/*.c ${CMAKE_CURRENT_SOURCE_DIR}/src/main/jni/*.h)
# 添加jni中转库
add_library(grpc_native_bridge SHARED ${JNI_SRC})
# 找到对应cpu的so库文件
file(GLOB OUTPUT_LIBRARY jniLibs/${CMAKE_ANDROID_ARCH_ABI}/*.so)

target_link_libraries(
        grpc_native_bridge
        ${std-lib} # 连接android标准库给jni
        ${OUTPUT_LIBRARY} # 连接给jni库
)