package com.github.kr328.clash.core.model

import android.os.Parcel
import android.os.Parcelable
import com.github.kr328.clash.core.util.Parcelizer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConfigurationOverride(
    @SerialName("port")
    var httpPort: Int? = null,

    @SerialName("socks-port")
    var socksPort: Int? = null,

    @SerialName("redir-port")
    var redirectPort: Int? = null,

    @SerialName("tproxy-port")
    var tproxyPort: Int? = null,

    @SerialName("mixed-port")
    var mixedPort: Int? = null,

    @SerialName("authentication")
    var authentication: List<String>? = null,

    @SerialName("allow-lan")
    var allowLan: Boolean? = null,

    @SerialName("bind-address")
    var bindAddress: String? = null,

    @SerialName("mode")
    var mode: TunnelState.Mode? = null,

    @SerialName("log-level")
    var logLevel: LogMessage.Level? = null,

    @SerialName("ipv6")
    var ipv6: Boolean? = null,

    @SerialName("hosts")
    var hosts: Map<String, String>? = null,

    @SerialName("dns")
    val dns: Dns = Dns(),

    @SerialName("clash-for-android")
    val app: App = App(),
) : Parcelable {
    @Serializable
    data class Dns(
        @SerialName("enable")
        var enable: Boolean? = null,

        @SerialName("listen")
        var listen: String? = null,

        @SerialName("ipv6")
        var ipv6: Boolean? = null,

        @SerialName("use-hosts")
        var useHosts: Boolean? = null,

        @SerialName("enhanced-mode")
        var enhancedMode: DnsEnhancedMode? = null,

        @SerialName("nameserver")
        var nameServer: List<String>? = null,

        @SerialName("fallback")
        var fallback: List<String>? = null,

        @SerialName("default-nameserver")
        var defaultServer: List<String>? = null,

        @SerialName("fake-ip-filter")
        var fakeIpFilter: List<String>? = null,

        @SerialName("fallback-filter")
        val fallbackFilter: DnsFallbackFilter = DnsFallbackFilter(),

        @SerialName("nameserver-policy")
        var nameserverPolicy: Map<String, String>? = null,
    )

    @Serializable
    data class DnsFallbackFilter(
        @SerialName("geoip")
        var geoIp: Boolean? = null,

        @SerialName("ipcidr")
        var ipcidr: List<String>? = null,

        @SerialName("domain")
        var domain: List<String>? = null,
    )

    @Serializable
    data class App(
        @SerialName("append-system-dns")
        var appendSystemDns: Boolean? = null
    )

    @Serializable
    enum class DnsEnhancedMode {
        @SerialName("normal")
        None,

        @SerialName("redir-host")
        Mapping,

        @SerialName("fake-ip")
        FakeIp,
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        Parcelizer.encodeToParcel(serializer(), parcel, this)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ConfigurationOverride> {
        override fun createFromParcel(parcel: Parcel): ConfigurationOverride {
            return Parcelizer.decodeFromParcel(serializer(), parcel)
        }

        override fun newArray(size: Int): Array<ConfigurationOverride?> {
            return arrayOfNulls(size)
        }
    }
}