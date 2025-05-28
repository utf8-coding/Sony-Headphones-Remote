//
// Created by utf8coding on 5/27/2025.
//

#include "HelloWorldLib.h"
#include <string>
void HelloWorldLib::cppHelloWorld(const std::string& nameString) {
    JNIEnv* env = getEnvFromJVM(m_vm);

    LOGW("hello world from cpp %s", nameString.c_str());

    // Now callback kotlin!
    env->CallVoidMethod(m_javaCallbackGlobalRef, m_callbackMethodId, env->NewStringUTF(nameString.c_str()));
}

HelloWorldLib::HelloWorldLib(JNIEnv *env, jobject globalCallbackObj): m_javaCallbackGlobalRef(nullptr),
                                                                 m_callbackMethodId(nullptr) {
    LOGD("HelloWorldLib created");
    env->GetJavaVM(&m_vm);
    m_javaCallbackGlobalRef = globalCallbackObj;
    if (m_javaCallbackGlobalRef == nullptr) {
        LOGE("Failed to create global reference for Java callback object!");
        return;
    }

    jclass callback_class = env->GetObjectClass(m_javaCallbackGlobalRef);
    if (callback_class == nullptr) {
        LOGE("Failed to get class of Java callback object!");
        env->DeleteGlobalRef(m_javaCallbackGlobalRef);
        m_javaCallbackGlobalRef = nullptr;
        return;
    }

    m_callbackMethodId = env->GetMethodID(callback_class, "onCallBack", "(Ljava/lang/String;)V");
    if (m_callbackMethodId == nullptr) {
        LOGE("Failed to get method ID for onCallBack!");
        env->DeleteGlobalRef(m_javaCallbackGlobalRef);
        m_javaCallbackGlobalRef = nullptr;
        env->DeleteLocalRef(callback_class);
        return;
    }
    env->DeleteLocalRef(callback_class);
}

void HelloWorldLib::clean() {
    JNIEnv* env = getEnvFromJVM(m_vm);
    env->DeleteGlobalRef(m_javaCallbackGlobalRef);
}

HelloWorldLib::~HelloWorldLib(){
    clean();
}

extern "C" {
    JNIEXPORT jlong JNICALL
    Java_com_example_sonyheadphonesremote_cpp_wrapper_HelloWorldWrapper_createNativeObj(JNIEnv *env,
                                                                                        jobject thiz,
                                                                                        jobject callbackObj) {
        jobject globalRefCallbackObj = env->NewGlobalRef(callbackObj);
#pragma clang diagnostic push
#pragma ide diagnostic ignored "MemoryLeak"
        auto *native_obj = new HelloWorldLib(env, globalRefCallbackObj);
#pragma clang diagnostic pop

        return jlong(native_obj);
    }
    JNIEXPORT void JNICALL
    Java_com_example_sonyheadphonesremote_cpp_wrapper_HelloWorldWrapper_deleteNativeObj(JNIEnv *env,
                                                                                        jobject thiz,
                                                                                        jlong nativeCppObjPtr) {
        auto *native_obj = reinterpret_cast<HelloWorldLib *>(nativeCppObjPtr);
        delete native_obj;
    }
    JNIEXPORT void JNICALL
    Java_com_example_sonyheadphonesremote_cpp_wrapper_HelloWorldWrapper_showHelloWorld(JNIEnv *env,
                                                                                       jobject thiz,
                                                                                       jlong native_cpp_obj_ptr,
                                                                                       jstring name) {
        auto* native_obj = reinterpret_cast<HelloWorldLib *>(native_cpp_obj_ptr);
        std::string name_string = env->GetStringUTFChars(name, nullptr);
        native_obj->cppHelloWorld(name_string);
    }
}
