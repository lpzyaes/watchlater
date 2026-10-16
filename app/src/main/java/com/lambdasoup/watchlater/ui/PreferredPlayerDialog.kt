package com.lambdasoup.watchlater.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lambdasoup.watchlater.R
import com.lambdasoup.watchlater.data.YoutubeRepository

object PreferredPlayerDialog {

    private const val ACTION_CUSTOM_PACKAGE = "action_custom_package"

    data class PlayerOption(
        val title: String,
        val packageName: String,
        val subtitle: String,
        val icon: Drawable?,
        val isInstalled: Boolean,
        val isCustomAction: Boolean = false
    )

    fun show(
        activity: AppCompatActivity,
        youtubeRepository: YoutubeRepository,
        onSelected: (() -> Unit)? = null
    ) {
        val pm = activity.packageManager
        val currentPackage = youtubeRepository.getPreferredPackage()
        val options = mutableListOf<PlayerOption>()

        // 1. Official YouTube
        val ytInstalled = isAppInstalled(pm, YoutubeRepository.PACKAGE_DEFAULT_YOUTUBE)
        val ytIcon = getAppIcon(activity, pm, YoutubeRepository.PACKAGE_DEFAULT_YOUTUBE, R.drawable.ic_play_arrow)
        val ytSubtitle = if (ytInstalled) {
            YoutubeRepository.PACKAGE_DEFAULT_YOUTUBE
        } else {
            "${YoutubeRepository.PACKAGE_DEFAULT_YOUTUBE} (${activity.getString(R.string.player_not_installed)})"
        }
        options.add(
            PlayerOption(
                title = activity.getString(R.string.player_official_youtube),
                packageName = YoutubeRepository.PACKAGE_DEFAULT_YOUTUBE,
                subtitle = ytSubtitle,
                icon = ytIcon,
                isInstalled = ytInstalled
            )
        )

        // 2. System App Chooser
        val chooserIcon = ContextCompat.getDrawable(activity, R.drawable.ic_launcher_foreground)
        options.add(
            PlayerOption(
                title = activity.getString(R.string.player_system_chooser),
                packageName = YoutubeRepository.PACKAGE_SYSTEM_CHOOSER,
                subtitle = activity.getString(R.string.player_system_chooser_desc),
                icon = chooserIcon,
                isInstalled = true
            )
        )

        // 3. Known alternatives if installed
        val knownAlternatives = listOf(
            "org.schabi.newpipe" to "NewPipe",
            "app.revanced.android.youtube" to "YouTube ReVanced",
            "com.github.libretube" to "LibreTube",
            "org.polymorphicshade.newpipe" to "NewPipe (SponsorBlock)",
            "com.brave.browser" to "Brave Browser",
            "org.mozilla.firefox" to "Firefox",
            "org.videolan.vlc" to "VLC"
        )
        for ((pkg, name) in knownAlternatives) {
            if (isAppInstalled(pm, pkg)) {
                options.add(
                    PlayerOption(
                        title = name,
                        packageName = pkg,
                        subtitle = pkg,
                        icon = getAppIcon(activity, pm, pkg, R.drawable.ic_play_arrow),
                        isInstalled = true
                    )
                )
            }
        }

        // 4. Other installed apps that resolve YouTube links
        try {
            val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=EyYb7obDgcI"))
            val resolvedList = pm.queryIntentActivities(viewIntent, 0)
            for (resolveInfo in resolvedList) {
                val pkg = resolveInfo.activityInfo.packageName
                if (pkg != activity.packageName && options.none { it.packageName == pkg }) {
                    val label = resolveInfo.loadLabel(pm).toString()
                    val icon = resolveInfo.loadIcon(pm)
                    options.add(
                        PlayerOption(
                            title = label,
                            packageName = pkg,
                            subtitle = pkg,
                            icon = icon,
                            isInstalled = true
                        )
                    )
                }
            }
        } catch (ignored: Exception) {
        }

        // 5. If current package is not in the list (e.g. user previously entered a custom package)
        if (currentPackage.isNotEmpty() &&
            currentPackage != YoutubeRepository.PACKAGE_DEFAULT_YOUTUBE &&
            currentPackage != YoutubeRepository.PACKAGE_SYSTEM_CHOOSER &&
            options.none { it.packageName == currentPackage }) {
            val installed = isAppInstalled(pm, currentPackage)
            val icon = getAppIcon(activity, pm, currentPackage, R.drawable.ic_play_arrow)
            val subtitle = if (installed) currentPackage else "$currentPackage (${activity.getString(R.string.player_not_installed)})"
            options.add(
                PlayerOption(
                    title = activity.getString(R.string.player_custom_current_title, currentPackage),
                    packageName = currentPackage,
                    subtitle = subtitle,
                    icon = icon,
                    isInstalled = installed
                )
            )
        }

        // 6. Custom package option
        val editIcon = ContextCompat.getDrawable(activity, R.drawable.ic_edit)
        options.add(
            PlayerOption(
                title = activity.getString(R.string.player_custom_option),
                packageName = ACTION_CUSTOM_PACKAGE,
                subtitle = activity.getString(R.string.player_custom_option_desc),
                icon = editIcon,
                isInstalled = true,
                isCustomAction = true
            )
        )

        // Adapter
        val adapter = object : ArrayAdapter<PlayerOption>(activity, R.layout.item_player_choice, options) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_player_choice, parent, false)
                val item = getItem(position)!!
                val iconView = view.findViewById<ImageView>(R.id.player_item_icon)
                val titleView = view.findViewById<TextView>(R.id.player_item_title)
                val packageView = view.findViewById<TextView>(R.id.player_item_package)
                val checkView = view.findViewById<ImageView>(R.id.player_item_check)

                if (item.icon != null) {
                    iconView.setImageDrawable(item.icon)
                    if (item.isCustomAction) {
                        iconView.setColorFilter(ContextCompat.getColor(context, R.color.mat_lightblue_600))
                    } else {
                        iconView.clearColorFilter()
                    }
                } else {
                    iconView.setImageResource(R.drawable.ic_play_arrow)
                    iconView.setColorFilter(ContextCompat.getColor(context, R.color.mat_lightblue_600))
                }

                titleView.text = item.title
                packageView.text = item.subtitle

                val isSelected = !item.isCustomAction && item.packageName == currentPackage
                checkView.visibility = if (isSelected) View.VISIBLE else View.GONE
                checkView.setColorFilter(ContextCompat.getColor(context, R.color.mat_lightblue_600))

                return view
            }
        }

        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.dialog_preferred_player_title)
            .setAdapter(adapter) { _, which ->
                val selected = options[which]
                if (selected.isCustomAction) {
                    showCustomPackageInputDialog(activity, youtubeRepository, onSelected)
                } else {
                    youtubeRepository.setPreferredPackage(selected.packageName)
                    val toastMsg = activity.getString(R.string.preferred_player_saved, selected.title)
                    Toast.makeText(activity, toastMsg, Toast.LENGTH_SHORT).show()
                    onSelected?.invoke()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showCustomPackageInputDialog(
        activity: AppCompatActivity,
        youtubeRepository: YoutubeRepository,
        onSelected: (() -> Unit)?
    ) {
        val currentPackage = youtubeRepository.getPreferredPackage()
        val defaultText = if (currentPackage != YoutubeRepository.PACKAGE_DEFAULT_YOUTUBE &&
            currentPackage != YoutubeRepository.PACKAGE_SYSTEM_CHOOSER) {
            currentPackage
        } else {
            ""
        }

        val editText = EditText(activity).apply {
            hint = activity.getString(R.string.dialog_custom_package_hint)
            setText(defaultText)
            setSelection(text.length)
            setSingleLine()
            setTextColor(ContextCompat.getColor(context, R.color.dialog_text_primary))
            setHintTextColor(ContextCompat.getColor(context, R.color.dialog_text_secondary))
            val padding = (16 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
        }

        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.dialog_custom_package_title)
            .setMessage(R.string.dialog_custom_package_message)
            .setView(editText)
            .setPositiveButton(R.string.dialog_save) { _, _ ->
                val entered = editText.text.toString().trim()
                if (entered.isNotEmpty()) {
                    youtubeRepository.setPreferredPackage(entered)
                    Toast.makeText(
                        activity,
                        activity.getString(R.string.preferred_player_saved, entered),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    youtubeRepository.setPreferredPackage(YoutubeRepository.PACKAGE_DEFAULT_YOUTUBE)
                    Toast.makeText(activity, R.string.preferred_player_reset, Toast.LENGTH_SHORT).show()
                }
                onSelected?.invoke()
            }
            .setNeutralButton(R.string.dialog_reset_youtube) { _, _ ->
                youtubeRepository.setPreferredPackage(YoutubeRepository.PACKAGE_DEFAULT_YOUTUBE)
                Toast.makeText(activity, R.string.preferred_player_reset, Toast.LENGTH_SHORT).show()
                onSelected?.invoke()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun isAppInstalled(pm: PackageManager, packageName: String): Boolean {
        return try {
            pm.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun getAppIcon(context: Context, pm: PackageManager, packageName: String, fallbackRes: Int): Drawable? {
        return try {
            pm.getApplicationIcon(packageName)
        } catch (e: Exception) {
            ContextCompat.getDrawable(context, fallbackRes)
        }
    }
}
