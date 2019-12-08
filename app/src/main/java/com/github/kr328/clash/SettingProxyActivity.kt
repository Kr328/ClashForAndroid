package com.github.kr328.clash

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.annotation.Keep
import androidx.core.content.edit
import androidx.preference.CheckBoxPreference
import androidx.preference.PreferenceFragmentCompat
import kotlinx.android.synthetic.main.activity_setting_proxy.*

class SettingProxyActivity : ToolbarActivity() {
    companion object {
        private const val KEY_BYPASS_PRIVATE_NETWORK = "key_vpn_setting_bypass_private_network"
        private const val KEY_IPV6_SUPPORT = "key_vpn_setting_ipv6_support"
    }

    @Keep
    class Fragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.setting_proxy, rootKey)
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            (requireActivity() as SettingProxyActivity).runClash {
                val settings = it.settingService

                val ipv6 = settings.isIPv6Enabled
                val privateNetwork = settings.isBypassPrivateNetwork

                requireActivity().runOnUiThread {
                    findPreference<CheckBoxPreference>(KEY_IPV6_SUPPORT)?.isChecked = ipv6
                    findPreference<CheckBoxPreference>(KEY_BYPASS_PRIVATE_NETWORK)?.isChecked =
                        privateNetwork
                }
            }
        }

        override fun onStop() {
            super.onStop()

            (requireActivity() as SettingProxyActivity).runClash {
                val settings = it.settingService

                settings.isIPv6Enabled =
                    findPreference<CheckBoxPreference>(KEY_IPV6_SUPPORT)?.isChecked ?: true
                settings.isBypassPrivateNetwork =
                    findPreference<CheckBoxPreference>(KEY_BYPASS_PRIVATE_NETWORK)?.isChecked
                        ?: true
            }
        }
    }

    override fun onStop() {
        super.onStop()

        getSharedPreferences("application", Context.MODE_PRIVATE).edit {
            when {
                activity_setting_proxy_vpn_mode.isChecked ->
                    putString(MainApplication.KEY_PROXY_MODE, MainApplication.PROXY_MODE_VPN)
                activity_setting_proxy_proxy_only_mode.isChecked ->
                    putString(MainApplication.KEY_PROXY_MODE, MainApplication.PROXY_MODE_PROXY_ONLY)
            }
        }
    }

    override fun initData(bundle: Bundle?) {
    }

    override fun bindLayout(): Int {
        return R.layout.activity_setting_proxy
    }

    override fun initView(savedInstanceState: Bundle?, contentView: View?) {
        activity_setting_proxy_vpn_mode.setOnClickListener {
            activity_setting_proxy_vpn_mode.isChecked = true
            activity_setting_proxy_proxy_only_mode.isChecked = false

            activity_setting_proxy_divider.visibility = View.VISIBLE
            activity_setting_proxy_content.visibility = View.VISIBLE
        }

        activity_setting_proxy_proxy_only_mode.setOnClickListener {
            activity_setting_proxy_vpn_mode.isChecked = false
            activity_setting_proxy_proxy_only_mode.isChecked = true

            activity_setting_proxy_divider.visibility = View.GONE
            activity_setting_proxy_content.visibility = View.GONE
        }
    }

    override fun doBusiness() {
        getSharedPreferences("application", Context.MODE_PRIVATE).apply {
            when (getString(MainApplication.KEY_PROXY_MODE, MainApplication.PROXY_MODE_VPN)) {
                MainApplication.PROXY_MODE_VPN ->
                    activity_setting_proxy_vpn_mode.performClick()
                MainApplication.PROXY_MODE_PROXY_ONLY ->
                    activity_setting_proxy_proxy_only_mode.performClick()
            }
        }
    }
}