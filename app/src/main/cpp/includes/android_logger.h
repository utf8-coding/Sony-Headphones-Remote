//
// Created by utf8coding on 5/27/2025.
//

#ifndef SONY_HEADPHONES_REMOTE_ANDROID_LOGGER_H
#define SONY_HEADPHONES_REMOTE_ANDROID_LOGGER_H

#include <android/log.h>
#define LOG_TAG "Native: "
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#endif //SONY_HEADPHONES_REMOTE_ANDROID_LOGGER_H
