[TOC]

## 1 一个JNI的调用-回调框架，和相应的工具类示例

### 1. Kotlin/Java 端 (`MainActivity.kt` & `ProcessorWrapperCallback.kt` & `ProessorWrapper.kt`)

#### **`app/src/main/java/com/example/mynativeapp/ProcessorWrapperCallback.kt` (Kotlin 回调接口)**

```Kotlin
package com.example.mynativeapp

// 定义一个 Kotlin 接口，供 C++ 调用
interface ProcessorWrapperCallback {
    fun onProcessedData(data: String, value: Int)
    fun onError(errorCode: Int, message: String)
}
```

#### **`app/src/main/java/com/example/mynativeapp/ProessorWrapper.kt` (Kotlin JNI 桥接（Wrapper）类)**

```Kotlin
package com.example.mynativeapp

import android.util.Log

// 封装 JNI 调用，作为 C++ 对象的代理
class ProessorWrapper(callBack: ProcessorWrapperCallback) {

    // nativeHandle 将存储 C++ MyProcessor 对象的内存地址
    private var nativeHandle: Long = 0

    init {
        System.loadLibrary("mynativeapp")
        nativeCreateProcessor(callBack)
    }

    // 用于设置/获取 nativeHandle，因为 nativeCreateProcessor 是伴生对象方法
    fun setNativeHandle(handle: Long) {
        this.nativeHandle = handle
        Log.d("ProessorWrapper", "ProessorWrapper instance got native handle: $nativeHandle")
    }

    fun process(input: String) {
        if (nativeHandle != 0L) {
            nativeProcess(nativeHandle, input)
        } else {
            Log.e("ProessorWrapper", "Cannot call process on an invalid CppProcessor (handle is 0).")
        }
    }

    fun destroy() {
        if (nativeHandle != 0L) {
            nativeDestroyProcessor(nativeHandle)
            nativeHandle = 0 // 清空句柄
            Log.d("ProessorWrapper", "CppProcessor instance destroyed, native handle cleared.")
        }
    }

    // 声明 JNI Native 方法 (实例方法)
    private external fun nativeCreateProcessor(callback: ProcessorWrapperCallback): Long
    private external fun nativeProcess(handle: Long, input: String)
    private external fun nativeDestroyProcessor(handle: Long)

    @Suppress("Finalize")
    protected fun finalize() {
        if (nativeHandle != 0L) {
            Log.w("ProessorWrapper", "ProessorWrapper was finalized without explicit destroy() call. Destroying native object.")
            destroy() // 确保在垃圾回收时释放 C++ 资源
        }
    }
}
```

#### **`app/src/main/java/com/example/mynativeapp/MainActivity.kt` (UI 逻辑)**

```Kotlin
package com.example.mynativeapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView

class MainActivity : AppCompatActivity(), ProcessorWrapperCallback { // 实现回调接口

    private lateinit var textView: TextView
    private var nativeProcessor: ProessorWrapper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView = findViewById(R.id.textView)
        val createButton: Button = findViewById(R.id.createButton)
        val processButton: Button = findViewById(R.id.processButton)
        val destroyButton: Button = findViewById(R.id.destroyButton)

        createButton.setOnClickListener {
            if (nativeProcessor == null) {
                val handle = ProessorWrapper.nativeCreateProcessor(object: ProcessorWrapperCCallback {
                    // 实现 ProcessorWrapperCallback 接口方法，这些方法将被 C++ 调用
                    override fun onProcessedData(data: String, value: Int) {
                        runOnUiThread { // 确保在主线程更新 UI
                            textView.text = "Callback: Processed Data: '$data', Value: $value"
                            Log.d("MainActivity", "onProcessedData: $data, $value")
                        }
                    }

                    override fun onError(errorCode: Int, message: String) {
                        runOnUiThread { // 确保在主线程更新 UI
                            textView.text = "Callback: Error! Code: $errorCode, Message: $message"
                            Log.e("MainActivity", "onError: $errorCode, $message")
                        }
                    }

                    override fun onDestroy() {
                        super.onDestroy()
                        nativeProcessor?.destroy()
                        nativeProcessor = null
                        Log.d("MainActivity", "Activity destroyed, ensuring C++ Processor cleanup.")
                    }
                }) // 将 MainActivity 作为回调传递
                if (handle != 0L) {
                    nativeProcessor = ProcessorWrapper()
                    nativeProcessor?.setNativeHandle(handle)
                    textView.text = "C++ Processor created."
                    Log.d("MainActivity", "C++ Processor created with handle: $handle")
                } else {
                    textView.text = "Failed to create C++ Processor."
                    Log.e("MainActivity", "Failed to create C++ Processor, handle is 0.")
                }
            } else {
                textView.text = "C++ Processor already exists."
            }
        }

        processButton.setOnClickListener {
            nativeProcessor?.let {
                val inputData = "Hello from Kotlin at ${System.currentTimeMillis()}"
                it.process(inputData) // 调用 C++ 对象的处理方法
                textView.text = "Sent '$inputData' to C++ Processor."
            } ?: run {
                textView.text = "Create C++ Processor first!"
            }
        }

        destroyButton.setOnClickListener {
            nativeProcessor?.let {
                it.destroy()
                nativeProcessor = null
                textView.text = "C++ Processor destroyed."
                Log.d("MainActivity", "C++ Processor destroyed.")
            } ?: run {
                textView.text = "No C++ Processor to destroy."
            }
        }
    }
}
```

### 2. C++ 端 (`app/src/main/cpp/my_processor.h` & `my_processor.cpp` & `native-lib.cpp`)

#### **`app/src/main/cpp/my_processor.h` (C++ 类定义)**

```C++
#pragma once
#include <string>
#include <jni.h> // 需要 JNI 类型
#include <memory> // For std::unique_ptr or std::shared_ptr if managing resources

class MyProcessor {
public:
    // 构造函数：接受 JNIEnv* 和 Java 回调对象
    // JavaVM* vm; // 存储 JVM 实例，用于在其他线程获取 JNIEnv
    MyProcessor(JNIEnv* env, jobject callback_obj);
    ~MyProcessor();

    // 处理输入数据，并可能触发 Kotlin 回调
    void processData(JNIEnv* env, const std::string& input);

private:
    JavaVM* m_vm; // 保存 JVM 指针
    jobject m_javaCallbackGlobalRef; // Kotlin 回调对象的全局引用
    jmethodID m_onProcessedDataMethodId; // 缓存回调方法 ID
    jmethodID m_onErrorMethodId;         // 缓存错误回调方法 ID

    // 辅助函数：安全地获取 JNIEnv* 并附加线程
    JNIEnv* getJniEnvForCallback() const;
    void detachJniEnvForCallback(JNIEnv* env_to_detach) const;

    // 内部数据或状态
    int m_processedCount;
};
```

#### **`app/src/main/cpp/my_processor.cpp` (C++ 类实现)**

```c++
#include "my_processor.h"
#include <android/log.h>
#include <thread> // 模拟异步操作
#include <chrono> // 模拟延迟

#define LOG_TAG_CPP_PROCESSOR "MyProcessorCPP"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG_CPP_PROCESSOR, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG_CPP_PROCESSOR, __VA_ARGS__)

// 构造函数：获取 JNIEnv* 和 JavaVM*，创建全局引用，缓存方法 ID
MyProcessor::MyProcessor(JNIEnv* env, jobject callback_obj)
    : m_processedCount(0), m_javaCallbackGlobalRef(nullptr),
      m_onProcessedDataMethodId(nullptr), m_onErrorMethodId(nullptr) {

    env->GetJavaVM(&m_vm); // 获取并保存 JVM 指针，以便在任何线程使用

    // 创建 Kotlin 回调对象的全局引用
    m_javaCallbackGlobalRef = env->NewGlobalRef(callback_obj);
    if (m_javaCallbackGlobalRef == nullptr) {
        LOGE("Failed to create global reference for Java callback object!");
        return;
    }

    // 获取回调接口的 jclass
    jclass callback_class = env->GetObjectClass(m_javaCallbackGlobalRef);
    if (callback_class == nullptr) {
        LOGE("Failed to get class of Java callback object!");
        env->DeleteGlobalRef(m_javaCallbackGlobalRef); // 清理已创建的全局引用
        m_javaCallbackGlobalRef = nullptr;
        return;
    }

    // 缓存回调方法 ID
    m_onProcessedDataMethodId = env->GetMethodID(callback_class, "onProcessedData", "(Ljava/lang/String;I)V");
    m_onErrorMethodId = env->GetMethodID(callback_class, "onError", "(ILjava/lang/String;)V");

    if (m_onProcessedDataMethodId == nullptr || m_onErrorMethodId == nullptr) {
        LOGE("Failed to find callback method IDs!");
        // 清理资源
        env->DeleteLocalRef(callback_class);
        env->DeleteGlobalRef(m_javaCallbackGlobalRef);
        m_javaCallbackGlobalRef = nullptr;
        return;
    }

    env->DeleteLocalRef(callback_class); // 释放局部引用

    LOGI("MyProcessor instance created at %p with Java callback %p", this, m_javaCallbackGlobalRef);
}

// 析构函数：释放全局引用
MyProcessor::~MyProcessor() {
    if (m_javaCallbackGlobalRef != nullptr && m_vm != nullptr) {
        JNIEnv* env_to_detach = getJniEnvForCallback(); // 确保有 JNIEnv 来删除全局引用
        if (env_to_detach) {
            env_to_detach->DeleteGlobalRef(m_javaCallbackGlobalRef);
            LOGI("MyProcessor instance at %p: Java callback global ref deleted.", this);
        } else {
             LOGE("MyProcessor instance at %p: Could not get JNIEnv to delete global ref!", this);
        }
    }
    LOGI("MyProcessor instance at %p destroyed.", this);
}

// 辅助函数：安全地获取 JNIEnv* 并附加线程
JNIEnv* MyProcessor::getJniEnvForCallback() const {
    JNIEnv* env = nullptr;
    // JNI_VERSION_1_6 是大多数 Android NDK 项目支持的最低版本
    if (m_vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        // 如果当前线程未附加到 JVM，则附加
        if (m_vm->AttachCurrentThread(&env, nullptr) != JNI_OK) {
            LOGE("Failed to get or attach JNIEnv to current thread for callback!");
            return nullptr;
        }
        // 注意：这里没有 DetachCurrentThread，因为回调完成后需要保留线程
        // 在实际项目中，通常会在线程的生命周期结束后才 DetachCurrentThread
    }
    return env;
}

void MyProcessor::detachJniEnvForCallback(JNIEnv* env_to_detach) const {
    if (m_vm && env_to_detach) {
        m_vm->DetachCurrentThread();
    }
}


// 模拟处理数据，并在其中触发回调
void MyProcessor::processData(JNIEnv* env, const std::string& input) {
    LOGI("MyProcessor %p: Processing data '%s'", this, input.c_str());
    m_processedCount++;

    // 假设进行一些耗时操作
    std::this_thread::sleep_for(std::chrono::milliseconds(100));

    // 获取 JNIEnv*，用于回调 Java
    JNIEnv* callback_env = getJniEnvForCallback();
    if (callback_env && m_javaCallbackGlobalRef && m_onProcessedDataMethodId) {
        jstring processed_data_jstr = callback_env->NewStringUTF((input + " processed!").c_str());
        // 调用 Kotlin 回调方法
        callback_env->CallVoidMethod(m_javaCallbackGlobalRef,
                                     m_onProcessedDataMethodId,
                                     processed_data_jstr,
                                     static_cast<jint>(m_processedCount));
        callback_env->DeleteLocalRef(processed_data_jstr);
    } else {
        LOGE("MyProcessor %p: Failed to trigger onProcessedData callback!", this);
        // 如果回调失败，可以尝试调用 onError
        if (callback_env && m_javaCallbackGlobalRef && m_onErrorMethodId) {
            jstring error_msg = callback_env->NewStringUTF("Callback setup error!");
            callback_env->CallVoidMethod(m_javaCallbackGlobalRef, m_onErrorMethodId, static_cast<jint>(-1), error_msg);
            callback_env->DeleteLocalRef(error_msg);
        }
    }

    // 如果这个方法是在一个 C++ 独立线程中调用，需要手动 Detach
    // 但在这个例子中，这个方法是在 JNI 调用线程中被调用的，所以不需要 Detach
    // detachJniEnvForCallback(callback_env); // 不要在这里 Detach
}
```

#### **`app/src/main/cpp/native-lib.cpp` (JNI 接口)**

```C++
#include <jni.h>
#include <string>
#include "my_processor.h" // 包含我们的 C++ 类
#include <android/log.h>

#define LOG_TAG_JNI_LIB "NativeJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG_JNI_LIB, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG_JNI_LIB, __VA_ARGS__)


// 全局静态方法：创建 MyProcessor 实例并返回句柄
// 对应 Kotlin ProessorWrapper 伴生对象中的 nativeCreateProcessor
extern "C" JNIEXPORT jlong JNICALL
Java_com_example_mynativeapp_ProessorWrapper_Companion_nativeCreateProcessor( // 注意伴生对象的 JNI 命名
        JNIEnv* env,
        jobject /* this/clazz */, // 对于静态方法，这里是 Class 对象
        jobject callback_obj) { // Kotlin 回调对象
    MyProcessor* processor = new MyProcessor(env, callback_obj);
    if (processor == nullptr) {
        LOGE("Failed to create MyProcessor instance.");
        return 0;
    }
    LOGI("nativeCreateProcessor: New MyProcessor instance created at C++ address %p", processor);
    return reinterpret_cast<jlong>(processor); // 返回 C++ 指针作为句柄
}

// JNI 实例方法：调用 MyProcessor 的 processData 方法
// 对应 Kotlin ProessorWrapper 实例中的 nativeProcess
extern "C" JNIEXPORT void JNICALL
Java_com_example_mynativeapp_ProessorWrapper_nativeProcess(
        JNIEnv* env,
        jobject /* this */, // Kotlin ProessorWrapper 实例
        jlong handle,       // C++ MyProcessor 实例的句柄
        jstring input_jstr) { // 传入的字符串
    MyProcessor* processor = reinterpret_cast<MyProcessor*>(handle);
    if (processor) {
        const char* input_cstr = env->GetStringUTFChars(input_jstr, nullptr);
        std::string input_cpp(input_cstr);
        env->ReleaseStringUTFChars(input_jstr, input_cstr);

        processor->processData(env, input_cpp); // 调用 C++ 对象的成员函数
    } else {
        LOGE("nativeProcess: Invalid MyProcessor handle received!");
    }
}

// JNI 实例方法：销毁 MyProcessor 实例
// 对应 Kotlin ProessorWrapper 实例中的 nativeDestroyProcessor
extern "C" JNIEXPORT void JNICALL
Java_com_example_mynativeapp_ProessorWrapper_nativeDestroyProcessor(
        JNIEnv* env,
        jobject /* this */,
        jlong handle) {
    MyProcessor* processor = reinterpret_cast<MyProcessor*>(handle);
    if (processor) {
        delete processor; // 销毁 C++ 实例，其析构函数会释放 Java 回调的全局引用
        LOGI("nativeDestroyProcessor: MyProcessor instance at C++ address %p destroyed", processor);
    } else {
        LOGE("nativeDestroyProcessor: Attempted to destroy invalid MyProcessor handle!");
    }
}
```

### 3. CMake 配置 (`app/src/main/cpp/CMakeLists.txt`)

```CMake
cmake_minimum_required(VERSION 3.22.1)

project("mynativeapp")

# 将 my_processor.cpp 和 native-lib.cpp 都加入到源文件列表中
add_library( mynativeapp
             SHARED
             native-lib.cpp
             my_processor.cpp ) # <--- 添加 my_processor.cpp

# 添加头文件搜索路径，确保可以找到 my_processor.h
target_include_directories(mynativeapp PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}) # <--- 确保当前目录（包含 my_processor.h）被包含

find_library(log-lib log)

target_link_libraries(mynativeapp
                       ${log-lib} )
```

## 2. JNI Signature Cheat-sheet 

### 1. 基本类型（Primitive Types）的签名

每个 Java 基本类型都对应一个单字符的 JNI 签名：

| Java 类型                   | JNI 签名 | C/C++ 对应类型 |
| --------------------------- | -------- | -------------- |
| `boolean`                   | `Z`      | `jboolean`     |
| `byte`                      | `B`      | `jbyte`        |
| `char`                      | `C`      | `jchar`        |
| `short`                     | `S`      | `jshort`       |
| `int`                       | `I`      | `jint`         |
| `long`                      | `J`      | `jlong`        |
| `float`                     | `F`      | `jfloat`       |
| `double`                    | `D`      | `jdouble`      |
| `void` (仅用于方法返回类型) | `V`      | `void`         |

### 1.5 其他的JNI签名头为

| Java 类型                          | JNI 签名                       | 示例                                                         |
| ---------------------------------- | ------------------------------ | ------------------------------------------------------------ |
| 对象类型（包括类、接口、数组对象） | L                              | 以 `L` 开头，后跟类的完全限定名（使用 `/` 代替 `.` 作为包分隔符），最后以 `;` 结束。<br>**`java.lang.String`** 的签名：`Ljava/lang/String;`<br>**`java.util.List`** 的签名：`Ljava/util/List;` **自定义类<br>`com.example.MyClass`** 的签名：`Lcom/example/MyClass;` |
| JAVA数组                           | [                              | 以 `[` 开头，后面紧跟数组元素的类型签名。<br>**`int[]`** (int 数组) 的签名：`[I`<br>**`boolean[][]`** (boolean 的二维数组) 的签名：`[[Z`<br>**`java.lang.String[]`** (String 数组) 的签名：`[Ljava/lang/String;`<br>**`byte[][][]`** (byte 的三维数组) 的签名：`[[[B` |
| 字段签名                           | 它就是字段本身的类型签名       | `public int count;`：`I`                                     |
| 方法类型                           | (参数类型签名列表)返回类型签名 | 见后续                                                       |

### 2. 方法签名（Method Signatures）

方法签名是 JNI 中最复杂的部分。它由**参数类型**和**返回类型**组成，并遵循以下格式：`(参数类型签名列表)返回类型签名`

- **参数类型签名列表：** 将所有参数的类型签名按顺序写在括号 `()` 内，之间**没有分隔符**。
- **返回类型签名：** 在括号后面紧跟方法的返回类型签名。

**示例：**

| Java/Kotlin 方法声明                                      | JNI 签名                                                | 解释                                        |
| --------------------------------------------------------- | ------------------------------------------------------- | ------------------------------------------- |
| `void myMethod()`                                         | `()V`                                                   | 无参数，返回 `void`                         |
| `int add(int a, int b)`                                   | `(II)I`                                                 | 两个 `int` 参数，返回 `int`                 |
| `boolean isValid(String name)`                            | `(Ljava/lang/String;)Z`                                 | 一个 `String` 参数，返回 `boolean`          |
| `void processArray(byte[] data)`                          | `([B)V`                                                 | 一个 `byte[]` 参数，返回 `void`             |
| `long calculate(int value, String message, double ratio)` | `(ILjava/lang/String;D)J`                               | `int`, `String`, `double` 参数，返回 `long` |
| `String[] getNames(int count, boolean isActive)`          | `(IZ)[Ljava/lang/String;`                               | `int`, `boolean` 参数，返回 `String[]`      |
| `SomeObject create(AnotherObject obj)`                    | `(Lcom/example/AnotherObject;)Lcom/example/SomeObject;` | 自定义对象参数，返回自定义对象              |

**特殊方法：构造函数**

Java 类的构造函数在 JNI 中被特殊处理。它的方法名是 `<init>`，并且返回类型总是 `V` (void)。

- **`public MyClass()`** 的签名：`()V` (当然，获取 `MyClass` 的构造函数 ID 时，方法名需传入 `"<init>"`)
- **`public MyClass(String name, int id)`** 的签名：`(Ljava/lang/String;I)V` (方法名仍是 `"<init>"`)

### 实用工具：`javap`

手动构建 JNI 签名容易出错，尤其是对于复杂的方法和嵌套类型。Java SDK 提供了一个非常有用的命令行工具 `javap`，可以帮你生成类、方法和字段的 JNI 签名。

**使用方法：**

1. **编译你的 Java/Kotlin 代码：** 确保你已经有了 `.class` 文件。
2. 运行 `javap -s <FullyQualifiedClassName>`
   - 例如：`javap -s com.example.MyClass`

## 3. 调用一个函数

`CallVoidMethod` 和其他类似的 JNI 函数（例如 `CallBooleanMethod`, `CallIntMethod`, `CallObjectMethod` 等）是 JNI (Java Native Interface) 提供的一组核心功能，用于在 C/C++ 代码中**调用 Java/Kotlin 对象实例的非静态方法**。

它们的通用用法和目的可以总结为：

**目的：** 在 C/C++ 代码中，像 Java/Kotlin 代码调用自身方法一样，调用一个 Java/Kotlin 对象的特定方法。

**核心用法步骤：**

1. **获取 `JNIEnv*`：** 你需要一个有效的 `JNIEnv*` 指针。通常在 JNI 方法的第一个参数中获取，或者通过 `JavaVM*` 在 C++ 线程中获取。

2. **获取 `jobject` 实例：** 你需要调用哪个 Java/Kotlin 对象的实例。这通常是一个 `jobject` 类型的全局引用（如果你之前通过 `NewGlobalRef` 保存了 Java/Kotlin 对象）或者是一个局部引用（比如 JNI 方法的 `thiz` 参数，或者其他 JNI 函数返回的 `jobject`）。

3. 获取 `jmethodID`：

    你需要调用哪个方法。这通过 

   ```
   env->GetMethodID()
   ```

    函数获取，它需要：

   - 方法的 `jclass`（方法所属的类）
   - 方法名（C 风格字符串）
   - 方法签名（C 风格字符串，描述参数类型和返回类型）
   - **最佳实践：** `jmethodID` 通常在 JNI 初始化时（例如在你的 C++ 类的构造函数中）缓存起来，因为它在运行时是固定的，不需要每次调用都去查找，这样可以提高性能。

4. **根据方法签名准备参数：** 如果 Java/Kotlin 方法有参数，你需要将 C/C++ 类型转换为对应的 JNI 类型。

5. **调用 `CallMethod` 函数：** 使用正确的 `Call...Method` 函数族中的一个来执行调用。

**常见函数及其对应 Java/Kotlin 返回类型：**

| JNI 函数名          | Java/Kotlin 返回类型                 | 示例签名               |
| ------------------- | ------------------------------------ | ---------------------- |
| `CallVoidMethod`    | `void`                               | `()V`                  |
| `CallBooleanMethod` | `Boolean` (`jboolean`)               | `()Z`                  |
| `CallByteMethod`    | `Byte` (`jbyte`)                     | `()B`                  |
| `CallCharMethod`    | `Char` (`jchar`)                     | `()C`                  |
| `CallShortMethod`   | `Short` (`jshort`)                   | `()S`                  |
| `CallIntMethod`     | `Int` (`jint`)                       | `()I`                  |
| `CallLongMethod`    | `Long` (`jlong`)                     | `()J`                  |
| `CallFloatMethod`   | `Float` (`jfloat`)                   | `()F`                  |
| `CallDoubleMethod`  | `Double` (`jdouble`)                 | `()D`                  |
| `CallObjectMethod`  | 任何引用类型（`String`, 自定义类等） | `()Ljava/lang/String;` |