package com.github.kr328.clash

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.View
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import android.content.pm.PackageInfo
import com.github.kr328.clash.adapter.ProfileAdapter
import com.github.kr328.clash.core.event.ErrorEvent
import com.github.kr328.clash.core.event.ProfileChangedEvent
import com.github.kr328.clash.service.data.ClashProfileEntity
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_profiles.*
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import kotlin.concurrent.thread

class ProfilesActivity : BaseActivity() {
    private var dialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profiles)

        setSupportActionBar(activity_profiles_toolbar)

        activity_profiles_main_list.layoutManager = LinearLayoutManager(this)
        activity_profiles_main_list.adapter = ProfileAdapter(
            this,
            this::onProfileClick,
            this::onOperateClick,
            this::onProfileLongClick
        ) {
            startActivity(Intent(this, CreateProfileActivity::class.java))
        }
    }

    override fun onStart() {
        super.onStart()

        runClash {
            it.eventService.registerEventObserver(
                ProfilesActivity::class.java.simpleName,
                this,
                intArrayOf()
            )
        }

        reloadList()
    }

    override fun onStop() {
        super.onStop()

        runClash {
            it.eventService.unregisterEventObserver(ProfilesActivity::class.java.simpleName)
        }
    }

    override fun onProfileChanged(event: ProfileChangedEvent?) {
        reloadList()
    }

    private fun reloadList() {
        runClash {
            refreshList(it.profileService.queryProfiles())
        }
    }

    private fun refreshList(newData: Array<ClashProfileEntity>) {
        val adapter = activity_profiles_main_list.adapter as ProfileAdapter
        val oldData = adapter.profiles

        val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                oldData[oldItemPosition].id == newData[newItemPosition].id

            override fun getOldListSize(): Int = oldData.size

            override fun getNewListSize(): Int = newData.size

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                oldData[oldItemPosition] == newData[newItemPosition]
        })

        runOnUiThread {
            adapter.profiles = newData
            result.dispatchUpdatesTo(adapter)
        }
    }

    private fun onProfileClick(profile: ClashProfileEntity) {
        runClash {
            it.profileService.setActiveProfile(profile.id)
        }
    }

    private fun onOperateClick(profile: ClashProfileEntity) {
        when {
            ClashProfileEntity.isUrlToken(profile.token) -> {
                dialog?.dismiss()

                dialog = AlertDialog.Builder(this)
                    .setTitle(R.string.clash_profile_updating)
                    .setView(R.layout.dialog_profile_updating)
                    .setCancelable(false)
                    .show()

                updateProfile(profile)
            }
            ClashProfileEntity.isFileToken(profile.token) -> {
                Snackbar.make(
                    activity_profiles_root,
                    R.string.not_implemented,
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun onProfileLongClick(parent: View, profile: ClashProfileEntity) {
        PopupMenu(this, parent).apply {
            setOnMenuItemClickListener { removeProfile(profile).run { true } }
            inflate(R.menu.menu_profile_popup)
            show()
        }
    }

    private fun removeProfile(profile: ClashProfileEntity) {
        runClash {
            it.profileService.removeProfile(profile.id)

            File(profile.file).delete()
        }
    }

    private fun updateProfile(profile: ClashProfileEntity) {
        val url = ClashProfileEntity.getUrl(profile.token)

        runClash {
            thread {
                try {
                    val connection = URL(url).openConnection()
                    connection.setRequestProperty(
                        getString(R.string.clash_profile_invalid,
                                  packageManager.getPackageInfo(packageName, 0).let(PackageInfo::versionName)
                                 )
                    )
                    val data = with(connection) {
                        connectTimeout = ImportUrlActivity.DEFAULT_TIMEOUT
                        connect()

                        getInputStream().bufferedReader().use {
                            it.readText()
                        }
                    }

                    val pipe = ParcelFileDescriptor.createPipe()

                    thread {
                        FileOutputStream(pipe[1].fileDescriptor).use {
                            it.write(data.toByteArray())
                        }
                        pipe[1].close()
                    }

                    val error = it.checkProfileValid(pipe[0])

                    pipe[0].close()

                    if (error != null)
                        throw Exception(error)

                    FileOutputStream(profile.file).use { outputStream ->
                        outputStream.write(data.toByteArray())
                    }

                    runClash { clash ->
                        clash.profileService.touchProfile(profile.id)
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Snackbar.make(
                            activity_profiles_root,
                            getString(R.string.clash_profile_invalid, e.toString()),
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }

                runOnUiThread {
                    if (dialog?.isShowing == true)
                        dialog?.dismiss()
                }
            }
        }
    }

    override fun onErrorEvent(event: ErrorEvent?) {
        Snackbar.make(activity_profiles_root, event?.message ?: "Unknown", Snackbar.LENGTH_LONG)
            .show()
    }
}
