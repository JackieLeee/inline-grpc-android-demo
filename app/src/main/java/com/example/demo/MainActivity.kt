package com.example.demo

import kotlin.random.Random
import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.grpc.client.GreeterClient
import com.example.demo.ui.theme.CrossPlatGrpcIPCAndroidClientTheme
import com.example.grpc.pb.greeter.DataStruct
import com.example.grpc.pb.greeter.HelloReply
import com.example.grpc.pb.greeter.HelloRequest
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CrossPlatGrpcIPCAndroidClientTheme(
                darkTheme = false
            ) {
                Scaffold(modifier = Modifier.fillMaxSize()) {
                    GrpcStarter(this)
                }
            }
        }
    }
}

@Composable
fun GrpcStarter(activity: MainActivity) {
    val coroutineScope = rememberCoroutineScope()
    val message = remember { mutableStateOf(HelloReply.newBuilder().build()) }
    val errMsg = remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 标题
        Text(
            text = "GRPC接口调用"
        )

        // 显示message.value.message
        if (message.value.message != "") {
            Text(text = message.value.message)
        }
        // 显示message.value.dataList
        message.value.dataList.forEachIndexed { index, data ->
            Text(text = "Data [$index]:$data")
        }
        // 按钮
        MainButton {
            coroutineScope.launch {
                try {
                    // 调用接口
                    GreeterClient.getInstance().sayHello(
                        helloRequest(),
                        replyStreamObserver(message, errMsg)
                    )
                } catch (e: Exception) {
                    errMsg.value = e.message.orEmpty()
                }

                // 启动失败
                if (errMsg.value != "") {
                    activity.showToast(errMsg.value)
                    return@launch
                }
                return@launch
            }
        }
        ErrMsgDisplay(errMsg)
    }
}

private fun replyStreamObserver(
    message: MutableState<HelloReply>,
    errMsg: MutableState<String>
) = object : StreamObserver<HelloReply> {
    override fun onNext(value: HelloReply) {
        message.value = value
    }

    override fun onError(t: Throwable?) {
        errMsg.value = t?.message.orEmpty()
    }

    override fun onCompleted() {}
}

private fun helloRequest(): HelloRequest? {
    val builder = HelloRequest.newBuilder()
    builder.setName(generateRandomString(10))
    for (i in 0..Random.nextInt(1, 3)) {
        val data = DataStruct.newBuilder()
        for (i in 0..Random.nextInt(1, 5)) {
            data.addMessage(generateRandomString(5))
        }
        builder.addData(data)
    }
    val request = builder.build()
    return request
}

@Composable
fun ErrMsgDisplay(errMsg: MutableState<String>) {
    // 错误信息
    if (errMsg.value != "") {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .background(Color.Red),
            text = "错误信息: ${errMsg.value}"
        )
    }
}

@Composable
fun MainButton(onClick: () -> Unit) {
    // 按钮
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF4CAF50)
        ),
        onClick = {
            onClick.invoke()
        },
    ) {
        Text(text = "调用GRPC")
    }
}

// 扩展方法用于显示Toast
fun MainActivity.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

// 生成随机字符串
fun generateRandomString(length: Int): String {
    val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
    return (1..length)
        .map { allowedChars.random() }
        .joinToString("")
}