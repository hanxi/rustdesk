package com.carriez.flutter_hbb

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.util.Log

class PermissionRequestTransparentActivity: Activity() {
    private val logTag = "permissionRequest"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(logTag, "onCreate PermissionRequestTransparentActivity: intent.action: ${intent.action}")

        when (intent.action) {
            ACT_REQUEST_MEDIA_PROJECTION -> {
                val mediaProjectionManager =
                    getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                val intent = mediaProjectionManager.createScreenCaptureIntent()
                startActivityForResult(intent, REQ_REQUEST_MEDIA_PROJECTION)

                // 请求 root 权限
                try {
                    val process = Runtime.getRuntime().exec("su")
                    val os = DataOutputStream(process.outputStream)

                    // 执行需要 root 权限的命令
                    os.writeBytes("echo 'Do something as root...'\n")
                    os.flush()

                    // 结束命令列表
                    os.writeBytes("exit\n")
                    os.flush()
                    process.waitFor()
                    if (process.exitValue() == 0) {
                        Log.d(logTag, "Acquired root permissions.")
                        // 处理你需要 root 权限进行的操作
                    } else {
                        Log.d(logTag, "Failed to acquire root permissions.")
                    }
                } catch (e: IOException) {
                    Log.e(logTag, "IOException while requesting root permissions.", e)
                } catch (e: InterruptedException) {
                    Log.e(logTag, "Interrupted while requesting root permissions.", e)
                } finally {
                    finish()
                }
            }
            else -> finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_REQUEST_MEDIA_PROJECTION) {
            if (resultCode == RESULT_OK && data != null) {
                launchService(data)
            } else {
                setResult(RES_FAILED)
            }
        }

        finish()
    }

    private fun launchService(mediaProjectionResultIntent: Intent) {
        Log.d(logTag, "Launch MainService")
        val serviceIntent = Intent(this, MainService::class.java)
        serviceIntent.action = ACT_INIT_MEDIA_PROJECTION_AND_SERVICE
        serviceIntent.putExtra(EXT_MEDIA_PROJECTION_RES_INTENT, mediaProjectionResultIntent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

}
