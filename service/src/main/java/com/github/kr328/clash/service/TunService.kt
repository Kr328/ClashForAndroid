package com.github.kr328.clash.service

import android.app.PendingIntent
import android.content.Intent
import android.net.ProxyInfo
import android.net.VpnService
import android.os.Build
import com.github.kr328.clash.common.compat.pendingIntentFlags
import com.github.kr328.clash.common.constants.Components
import com.github.kr328.clash.common.log.Log
import com.github.kr328.clash.service.clash.clashRuntime
import com.github.kr328.clash.service.clash.module.*
import com.github.kr328.clash.service.model.AccessControlMode
import com.github.kr328.clash.service.store.ServiceStore
import com.github.kr328.clash.service.util.cancelAndJoinBlocking
import com.github.kr328.clash.service.util.parseCIDR
import com.github.kr328.clash.service.util.sendClashStarted
import com.github.kr328.clash.service.util.sendClashStopped
import kotlinx.coroutines.*
import kotlinx.coroutines.selects.select

class TunService : VpnService(), CoroutineScope by CoroutineScope(Dispatchers.Default) {
    private val self: TunService
        get() = this

    private var reason: String? = null

    private val runtime = clashRuntime {
        val store = ServiceStore(self)

        val close = install(CloseModule(self))
        val tun = install(TunModule(self))
        val config = install(ConfigurationModule(self))
        val network = install(NetworkObserveModule(self))
        val sideload = install(SideloadDatabaseModule(self))

        if (store.dynamicNotification)
            install(DynamicNotificationModule(self))
        else
            install(StaticNotificationModule(self))

        install(AppListCacheModule(self))
        install(SuspendModule(self))

        try {
            tun.open()

            while (isActive) {
                val quit = select<Boolean> {
                    close.onEvent {
                        true
                    }
                    config.onEvent {
                        reason = it.message

                        true
                    }
                    sideload.onEvent {
                        reason = it.message

                        true
                    }
                    network.onEvent { e ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                            setUnderlyingNetworks(e.network?.let { arrayOf(it) })
                        }

                        config.reload()

                        false
                    }
                }

                if (quit) break
            }
        } catch (e: Exception) {
            Log.e("Create clash runtime: ${e.message}", e)

            reason = e.message
        } finally {
            withContext(NonCancellable) {
                tun.close()

                stopSelf()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        if (StatusProvider.serviceRunning)
            return stopSelf()

        StatusProvider.serviceRunning = true

        StaticNotificationModule.createNotificationChannel(this)
        StaticNotificationModule.notifyLoadingNotification(this)

        runtime.launch()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        sendClashStarted()

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        TunModule.requestStop()

        StatusProvider.serviceRunning = false

        sendClashStopped(reason)

        cancelAndJoinBlocking()

        Log.i("TunService destroyed: ${reason ?: "successfully"}")

        super.onDestroy()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)

        runtime.requestGc()
    }

    private fun TunModule.open() {
        val store = ServiceStore(self)

        val device = with(Builder()) {
            // Interface address
            addAddress(TUN_GATEWAY, TUN_SUBNET_PREFIX)

            // Route
            if (store.bypassPrivateNetwork) {
                resources.getStringArray(R.array.bypass_private_route).map(::parseCIDR).forEach {
                    addRoute(it.ip, it.prefix)
                }

                // Route of virtual DNS
                addRoute(TUN_DNS, 32)
            } else {
                addRoute(NET_ANY, 0)
            }

            // Access Control
            when (store.accessControlMode) {
                AccessControlMode.AcceptAll -> Unit
                AccessControlMode.AcceptSelected -> {
                    (store.accessControlPackages + packageName).forEach {
                        runCatching { addAllowedApplication(it) }
                    }
                }
                AccessControlMode.DenySelected -> {
                    (store.accessControlPackages - packageName).forEach {
                        runCatching { addDisallowedApplication(it) }
                    }
                }
            }

            // Blocking
            setBlocking(false)

            // Mtu
            setMtu(TUN_MTU)

            // Session Name
            setSession("Clash")

            // Virtual Dns Server
            addDnsServer(TUN_DNS)

            // Open MainActivity
            setConfigureIntent(
                PendingIntent.getActivity(
                    self,
                    R.id.nf_vpn_status,
                    Intent().setComponent(Components.MAIN_ACTIVITY),
                    pendingIntentFlags(PendingIntent.FLAG_UPDATE_CURRENT)
                )
            )

            // Metered
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                setMetered(false)
            }

            // System Proxy
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && store.systemProxy) {
                listenHttp()?.let {
                    setHttpProxy(
                        ProxyInfo.buildDirectProxy(
                            it.address.hostAddress,
                            it.port,
                            if (store.bypassPrivateNetwork)
                                listOf(
                                    "localhost",
                                    "*.local",
                                    "127.*",
                                    "10.*",
                                    "172.16.*",
                                    "172.17.*",
                                    "172.18.*",
                                    "172.19.*",
                                    "172.2*",
                                    "172.30.*",
                                    "172.31.*",
                                    "192.168.*"
                                )
                            else
                                emptyList()
                        )
                    )
                }
            }

            val blocking = mutableListOf("$TUN_GATEWAY/$TUN_SUBNET_PREFIX")
            if (store.blockLoopback) {
                blocking.add(NET_SUBNET_LOOPBACK)
            }

            TunModule.TunDevice(
                fd = establish()?.detachFd()
                    ?: throw NullPointerException("Establish VPN rejected by system"),
                mtu = TUN_MTU,
                blocking = blocking.joinToString(";"),
                dns = if (store.dnsHijacking) NET_ANY else TUN_DNS,
            )
        }

        attach(device)
    }

    companion object {
        private const val TUN_MTU = 9000
        private const val TUN_SUBNET_PREFIX = 30
        private const val TUN_GATEWAY = "172.19.0.1"
        private const val TUN_DNS = "172.19.0.2"
        private const val NET_ANY = "0.0.0.0"
        private const val NET_SUBNET_LOOPBACK = "127.0.0.0/8"
    }
}