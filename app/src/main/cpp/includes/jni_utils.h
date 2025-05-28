//
// Created by utf8coding on 5/26/2025.
//

#ifndef SONY_HEADPHONES_REMOTE_JNI_UTILS_H
#define SONY_HEADPHONES_REMOTE_JNI_UTILS_H

#define JNI_VERSION JNI_VERSION_1_6

#include "android_logger.h"
#include <jni.h>

#include <stdexcept>

static JNIEnv *getEnvFromJVM(JavaVM *g_vm) {
    if (g_vm == nullptr) {
        throw std::runtime_error("JavaVM pointer is null");
    }
    JNIEnv *env;
    jint result = g_vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION);
    if (result == JNI_EDETACHED) {
        throw std::runtime_error("Current thread is not attached to the JVM");
    } else if (result == JNI_EVERSION) {
        throw std::runtime_error("JNI version not supported");
    } else if (result != JNI_OK) {
        throw std::runtime_error("Failed to get JNIEnv from JVM");
    }
    return env;
}

#endif //SONY_HEADPHONES_REMOTE_JNI_UTILS_H
