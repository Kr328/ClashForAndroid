package com.github.kr328.clash

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.YamlException
import com.github.kr328.clash.adapter.FormAdapter
import com.github.kr328.clash.model.ClashProfile
import com.github.kr328.clash.service.data.ClashProfileEntity
import com.github.kr328.clash.utils.FileUtils
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_import_url.*
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL
import kotlin.concurrent.thread

class ImportUrlActivity : BaseActivity() {
    companion object {
        const val DEFAULT_TIMEOUT = 30 * 1000
    }

    private val elements = listOf<FormAdapter.Type>(
        FormAdapter.TextType(
            R.drawable.ic_about,
            R.string.clash_profile_name,
            R.string.clash_profile_name_hint
        ),
        FormAdapter.TextType(
            R.drawable.ic_link,
            R.string.clash_profile_url,
            R.string.clash_profile_url_hint
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import_url)

        setSupportActionBar(activity_import_url_toolbar)

        activity_import_url_form.also {
            it.layoutManager = LinearLayoutManager(this)
            it.adapter = FormAdapter(this, elements)
        }

        activity_import_url_save.setOnClickListener {
            checkAndInsert()
        }

        if ( intent.action == Intent.ACTION_VIEW
            && intent.data?.scheme == "clash"
            && intent.data?.host == "install-config") {
            (elements[0] as FormAdapter.TextType).content =
                intent.data?.getQueryParameter("name") ?: ""
            (elements[1] as FormAdapter.TextType).content =
                intent.data?.getQueryParameter("url") ?: ""
        }

        setResult(Activity.RESULT_CANCELED)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data != null && (activity_import_url_form.adapter as FormAdapter)
                .onActivityResult(requestCode, resultCode, data)
        )
            return

        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun checkAndInsert() {
        try {
            val name = elements[0] as FormAdapter.TextType
            val url = elements[1] as FormAdapter.TextType

            if ( name.content.isBlank() ) {
                runOnUiThread{
                    Snackbar.make(
                        activity_import_url_root,
                        R.string.clash_import_empty_name,
                        Snackbar.LENGTH_LONG
                    ).show()
                }
                return
            }

            if ( url.content.isBlank() ) {
                runOnUiThread{
                    Snackbar.make(
                        activity_import_url_root,
                        R.string.clash_import_url_empty_url,
                        Snackbar.LENGTH_LONG
                    ).show()
                }
                return
            }

            activity_import_url_save.visibility = View.GONE
            activity_import_url_saving.visibility = View.VISIBLE

            runClash {
                val httpPort = it.queryGeneral().ports.randomHttp

                thread {
                    try {
                        val connection = if ( httpPort > 0 )
                            URL(url.content).openConnection(Proxy(Proxy.Type.HTTP,
                                InetSocketAddress.createUnresolved("127.0.0.1", httpPort)))
                        else
                            URL(url.content).openConnection()

                        val data = with (connection) {
                            connectTimeout = DEFAULT_TIMEOUT
                            connect()

                            getInputStream().bufferedReader().use {
                                it.readText()
                            }
                        }

                        Yaml(configuration = YamlConfiguration(strictMode = false)).parse(ClashProfile.serializer(), data)

                        val cache =
                            FileUtils.generateRandomFile(filesDir.resolve(Constants.PROFILES_DIR), ".yaml")

                        FileOutputStream(cache).use { outputStream ->
                            outputStream.write(data.toByteArray())
                        }

                        runClash { clash ->
                            clash.profileService.addProfile(ClashProfileEntity(name.content,
                                ClashProfileEntity.urlToken(url.content),
                                cache.absolutePath,
                                false,
                                System.currentTimeMillis()))
                        }

                        runOnUiThread {
                            setResult(Activity.RESULT_OK)

                            finish()
                        }
                    }
                    catch (e: Exception) {
                        runOnUiThread {
                            Snackbar.make(
                                activity_import_url_root,
                                getString(R.string.clash_profile_invalid, e.message?.replace(YamlException::class.java.name + ":", "") ?: "Unknown"),
                                Snackbar.LENGTH_LONG
                            ).show()

                            activity_import_url_save.visibility = View.VISIBLE
                            activity_import_url_saving.visibility = View.GONE
                        }
                    }
                }
            }


        }
        catch (e: Exception) {

        }
    }
}