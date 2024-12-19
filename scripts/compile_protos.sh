#!/bin/bash
# 定义颜色，用于日志输出
GREEN="\033[32m"
YELLOW="\033[33m"
RESET="\033[0m"

# 设置源目录和目标目录
SRC_DIR="../protobuf"
GEN_DIR="../app/src/main/java"

# 输出开始信息
echo -e "${YELLOW}Starting to compile .proto files...${RESET}"

# 使用 find 命令递归查找所有 .proto 文件并生成 Go 代码
find $SRC_DIR -name "*.proto" | while read -r proto_file; do
    # 获取相对路径
    relative_path=$(realpath --relative-to=$SRC_DIR "$proto_file")
    # 获取目录部分
    dir_path=$(dirname "$relative_path")

    # 确保生成目录存在
    if [ -n "$dir_path" ] && [ ! -d "$GEN_DIR/$dir_path" ]; then
        mkdir -p "$GEN_DIR/$dir_path"
        echo -e "${GREEN}Created directory: $GEN_DIR/$dir_path${RESET}"
    fi

    # 使用 protoc 编译 .proto 文件并生成 Go 代码
    echo -e "${YELLOW}Compiling: $relative_path${RESET}"
    protoc --proto_path=$SRC_DIR --java_out=$GEN_DIR --grpc-java_out=$GEN_DIR "$proto_file"
    echo -e "${GREEN}Compiled: $relative_path${RESET}"
done

# 输出结束信息
echo -e "${YELLOW}All .proto files compiled successfully!${RESET}"