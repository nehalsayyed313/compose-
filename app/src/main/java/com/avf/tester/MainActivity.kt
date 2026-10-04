package com.avf.tester

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.avf.tester.theme.AvfTesterTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile

// ==========================================
// 1. Data Models & Detector Logic
// ==========================================

data class AvfReport(
    val hasSystemFeature: Boolean = false,
    val isVmmServiceAvailable: Boolean = false,
    val capabilities: List<String> = emptyList(),
    val hypervisorVersion: String? = null,
    val isVmSupportedProp: String? = null,
    val isProtectedVmSupportedProp: String? = null,
    val hasDevKvm: Boolean = false,
    val canAccessDevKvm: Boolean = false,
    val cpuVirtualizationFlags: List<String> = emptyList(),
    val isAvfSupported: Boolean = false,
    val errors: List<String> = emptyList()
)

object AvfScanner {
    private const val FEATURE_VIRTUALIZATION = "android.software.virtualization_framework"
    private const val CAP_PROTECTED_VM = 1 shl 0
    private const val CAP_NON_PROTECTED_VM = 1 shl 1

    fun runCheck(context: Context): AvfReport {
        val errors = mutableListOf<String>()

        // 1. Package Manager Feature Check
        val hasFeature = context.packageManager.hasSystemFeature(FEATURE_VIRTUALIZATION)

        // 2. Safe Reflection for VirtualMachineManager (API 33+)
        var vmmAvailable = false
        val capsList = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val vmmClass = Class.forName("android.system.virtualmachine.VirtualMachineManager")
                val vmm = context.getSystemService(vmmClass)
                if (vmm != null) {
                    vmmAvailable = true
                    val capsMethod = vmmClass.getMethod("getCapabilities")
                    val caps = capsMethod.invoke(vmm) as? Int ?: 0
                    if ((caps and CAP_PROTECTED_VM) != 0) capsList.add("Protected VMs (pVM)")
                    if ((caps and CAP_NON_PROTECTED_VM) != 0) capsList.add("Non-Protected VMs")
                    if (capsList.isEmpty()) capsList.add("No capabilities declared")
                }
            } catch (t: Throwable) {
                errors.add("VMM query error: ${t.message ?: t.javaClass.simpleName}")
            }
        }

        // 3. Android System Properties
        val hvVersion = readSysProp("ro.boot.hypervisor.version")
        val vmProp = readSysProp("ro.boot.hypervisor.vm.supported")
        val pvmProp = readSysProp("ro.boot.hypervisor.protected_vm.supported")

        // 4. Inspect Kernel Node (/dev/kvm)
        val kvm = File("/dev/kvm")
        val exists = kvm.exists()
        var accessible = false
        if (exists) {
            try {
                RandomAccessFile(kvm, "rw").use { accessible = true }
            } catch (_: Exception) {
                accessible = kvm.canRead() || kvm.canWrite()
            }
        }

        // 5. Inspect CPU Virtualization Flags
        val flags = parseCpuFlags()

        val isFullySupported = hasFeature && (vmmAvailable || hvVersion != null)

        return AvfReport(
            hasSystemFeature = hasFeature,
            isVmmServiceAvailable = vmmAvailable,
            capabilities = capsList,
            hypervisorVersion = hvVersion,
            isVmSupportedProp = vmProp,
            isProtectedVmSupportedProp = pvmProp,
            hasDevKvm = exists,
            canAccessDevKvm = accessible,
            cpuVirtualizationFlags = flags,
            isAvfSupported = isFullySupported,
            errors = errors
        )
    }

    private fun readSysProp(key: String): String? {
        return try {
            val propClass = Class.forName("android.os.SystemProperties")
            val get = propClass.getMethod("get", String::class.java, String::class.java)
            val result = get.invoke(null, key, "") as? String
            if (result.isNullOrBlank()) null else result
        } catch (_: Throwable) {
            null
        }
    }

    private fun parseCpuFlags(): List<String> {
        val detected = mutableListOf<String>()
        try {
            val file = File("/proc/cpuinfo")
            if (file.exists()) {
                val content = file.readText().lowercase()
                for (flag in listOf("hyp", "el2", "vmx", "svm")) {
                    if (content.contains(flag)) detected.add(flag.uppercase())
                }
            }
        } catch (_: Exception) {}
        return detected
    }
}

// ==========================================
// 2. Main Activity & User Interface
// ==========================================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AvfTesterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AvfScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvfScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var report by remember { mutableStateOf<AvfReport?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    fun refresh() {
        isLoading = true
        scope.launch {
            report = withContext(Dispatchers.IO) { AvfScanner.runCheck(context) }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AVF Support Detector", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { refresh() }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Scan Again")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                report?.let { data ->
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Verdict Banner
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (data.isAvfSupported) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.errorContainer
                                    }
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (data.isAvfSupported) Icons.Default.CheckCircle else Icons.Default.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp),
                                        tint = if (data.isAvfSupported) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            text = if (data.isAvfSupported) "AVF Supported" else "AVF Not Supported",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (data.isAvfSupported) {
                                                "Hypervisor is active and AVF is enabled on this build."
                                            } else {
                                                "Virtualization framework is absent or disabled by OEM."
                                            },
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }

                        // Android Framework Checks
                        item {
                            CategoryHeader("Framework Layer")
                            MetricRow("System Feature", if (data.hasSystemFeature) "Declared" else "Missing", data.hasSystemFeature)
                            MetricRow("VMM System Service", if (data.isVmmServiceAvailable) "Available" else "Not Bound", data.isVmmServiceAvailable)
                            MetricRow("VMM Capabilities", if (data.capabilities.isEmpty()) "None" else data.capabilities.joinToString(", "), data.capabilities.isNotEmpty())
                        }

                        // Hypervisor & Kernel Checks
                        item {
                            CategoryHeader("Hypervisor & Kernel")
                            MetricRow("Hypervisor Version", data.hypervisorVersion ?: "Not Set", data.hypervisorVersion != null)
                            MetricRow("VM Supported (prop)", data.isVmSupportedProp ?: "Not Set", data.isVmSupportedProp == "1")
                            MetricRow("pVM Supported (prop)", data.isProtectedVmSupportedProp ?: "Not Set", data.isProtectedVmSupportedProp == "1")
                            MetricRow("/dev/kvm Node", if (data.hasDevKvm) "Present" else "Absent", data.hasDevKvm)
                            MetricRow("/dev/kvm App Access", if (data.canAccessDevKvm) "Accessible" else "Blocked (SELinux)", data.canAccessDevKvm)
                            MetricRow("CPU Flags", if (data.cpuVirtualizationFlags.isEmpty()) "None detected" else data.cpuVirtualizationFlags.joinToString(", "), data.cpuVirtualizationFlags.isNotEmpty())
                        }

                        // Device Details
                        item {
                            CategoryHeader("Build Environment")
                            MetricRow("Android Version", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})", Build.VERSION.SDK_INT >= 33)
                            MetricRow("Device Model", "${Build.MANUFACTURER} ${Build.MODEL}", null)
                        }

                        // Diagnostic Notes
                        if (data.errors.isNotEmpty()) {
                            item {
                                CategoryHeader("Diagnostic Errors")
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        data.errors.forEach { err ->
                                            Text(
                                                text = "• $err",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
fun MetricRow(label: String, value: String, isOk: Boolean?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = when (isOk) {
                true -> Color(0xFF2E7D32)
                false -> MaterialTheme.colorScheme.error
                null -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
    HorizontalDivider(thickness = 0.5.dp)
}
