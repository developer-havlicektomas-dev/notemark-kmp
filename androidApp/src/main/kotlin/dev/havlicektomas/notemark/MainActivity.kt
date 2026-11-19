package dev.havlicektomas.notemark

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi

class MainActivity : ComponentActivity() {

    private val requestLocalNetwork =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) requestLocalNetworkIfNeeded()

        setContent {
            App()
        }
    }

    /**
     * Android 17+ blocks local-network addresses (the dev backend at 10.0.2.2 /
     * on the LAN) unless ACCESS_LOCAL_NETWORK is granted. Only debug builds
     * declare the permission (src/debug/AndroidManifest.xml), so release builds
     * never prompt.
     */
    private fun requestLocalNetworkIfNeeded() {
        if (Build.VERSION.SDK_INT < 37) return
        val permission = Manifest.permission.ACCESS_LOCAL_NETWORK
        if (!declaresPermission(permission)) return
        if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) return
        requestLocalNetwork.launch(permission)
    }

    @RequiresApi(33)
    private fun declaresPermission(permission: String): Boolean {
        val info = packageManager.getPackageInfo(
            packageName,
            PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()),
        )
        return info.requestedPermissions?.contains(permission) == true
    }
}
