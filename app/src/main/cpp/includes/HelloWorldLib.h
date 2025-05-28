//
// Created by utf8coding on 5/27/2025.
// Calling this function and then calls the call back function
//

#ifndef SONY_HEADPHONES_REMOTE_HELLOWORLDLIB_H
#define SONY_HEADPHONES_REMOTE_HELLOWORLDLIB_H

#include "jni_utils.h"
#include <jni.h>

class HelloWorldLib {
public:
    HelloWorldLib(JNIEnv* env, jobject callback_obj);
    ~HelloWorldLib();
    void cppHelloWorld(const std::string& name);
    void clean();

private:
    JavaVM* m_vm; // 保存 JVM 指针
    jobject m_javaCallbackGlobalRef; // Kotlin 回调对象的全局引用
    jmethodID m_callbackMethodId; // 缓存回调方法 ID
};


#endif //SONY_HEADPHONES_REMOTE_HELLOWORLDLIB_H
