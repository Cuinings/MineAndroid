package com.cn.other.test

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlin.collections.get

/**
 * @author: cn
 * @time: 2026/1/29 16:15
 * @history
 * @description:
 */
class CameraCtrlActivity: AppCompatActivity() {

    private var cameraDevice: CameraDevice? = null
    private val CAMERA_PERMISSION_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ctrl_camera)
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        findViewById<Button>(R.id.camera_open).setOnClickListener {
            // 检查相机权限
            if (checkCameraPermission()) {
                // 已有权限，直接打开相机
                openCamera(cameraManager)
            } else {
                // 没有权限，请求相机权限
                requestCameraPermission()
            }
        }
        findViewById<Button>(R.id.camera_close).setOnClickListener {
            cameraDevice?.close()
            cameraDevice = null
        }
    }

    /**
     * 检查相机权限
     */
    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 请求相机权限
     */
    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
    }

    /**
     * 打开相机
     */
    private fun openCamera(cameraManager: CameraManager) {
        lifecycleScope.launch {
            try {
                // 通常选择后置摄像头，实际应用中应做更全面的检查
                if (cameraManager.cameraIdList.isNotEmpty()) {
                    val cameraId = cameraManager.cameraIdList[0]
                    cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                        override fun onOpened(camera: CameraDevice) {
                            // 相机成功打开，可以在此进行预览等后续操作
                            cameraDevice = camera
                            Toast.makeText(this@CameraCtrlActivity, "相机打开成功", Toast.LENGTH_SHORT).show()
                        }
                        override fun onDisconnected(camera: CameraDevice) {
                            camera.close()
                            cameraDevice = null
                            Toast.makeText(this@CameraCtrlActivity, "相机连接断开", Toast.LENGTH_SHORT).show()
                        }
                        override fun onError(camera: CameraDevice, error: Int) {
                            camera.close()
                            cameraDevice = null
                            Toast.makeText(this@CameraCtrlActivity, "相机打开失败", Toast.LENGTH_SHORT).show()
                        }
                    }, null)
                } else {
                    Toast.makeText(this@CameraCtrlActivity, "没有可用相机", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@CameraCtrlActivity, "打开相机时出错: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraDevice?.close()
        cameraDevice = null
    }

    /**
     * 处理权限请求结果
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 用户授予了相机权限
                val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
                openCamera(cameraManager)
            } else {
                // 用户拒绝了相机权限
                Toast.makeText(this, "需要相机权限才能打开相机", Toast.LENGTH_SHORT).show()
            }
        }
    }


}