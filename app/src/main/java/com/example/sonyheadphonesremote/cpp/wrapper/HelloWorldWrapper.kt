package com.example.sonyheadphonesremote.cpp.wrapper

import android.util.Log

interface HelloWorldCallback{
    fun onCallBack(intent: String)
}

class HelloWorldWrapper(): HelloWorldCallback {
    private var nativeCppObjPtr: Long? = null // Just be safe about this OK?
    init {
        System.loadLibrary("sonyheadphonesremote")
        nativeCppObjPtr = createNativeObj(this as HelloWorldCallback)
        showHelloWorld(nativeCppObjPtr!!, "utf8coding")
    }

    private fun onDestroy(){
        if(nativeCppObjPtr != null){
            deleteNativeObj(nativeCppObjPtr!!)
            nativeCppObjPtr = null
        }
    }

    // JNI Signature:(Ljava/lang/String;)V
    override fun onCallBack(intent: String) {
        Log.d("HelloWorldWrapper", "Hello world on callback: $intent")
    }

    private external fun createNativeObj(callback: HelloWorldCallback): Long
    private external fun deleteNativeObj(nativeCppObjPtr: Long)
    private external fun showHelloWorld(nativeCppObjPtr: Long, name: String)

}