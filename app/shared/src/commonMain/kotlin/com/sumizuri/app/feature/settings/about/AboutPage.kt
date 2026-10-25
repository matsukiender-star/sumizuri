package com.sumizuri.app.feature.settings.about

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.WavingHand
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sumizuri.app.BuildInfo
import com.sumizuri.app.data.LocalAppContainer
import com.sumizuri.app.data.appupdate.UpdateState
import com.sumizuri.app.data.settings.SettingKey
import com.sumizuri.app.feature.settings.SettingsPageFrame
import com.sumizuri.app.feature.settings.SettingsTarget
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.about_community
import com.sumizuri.app.shared.resources.about_discord
import com.sumizuri.app.shared.resources.about_discord_hint
import com.sumizuri.app.shared.resources.about_github
import com.sumizuri.app.shared.resources.about_github_hint
import com.sumizuri.app.shared.resources.about_issues
import com.sumizuri.app.shared.resources.about_issues_hint
import com.sumizuri.app.shared.resources.about_licenses
import com.sumizuri.app.shared.resources.about_licenses_hint
import com.sumizuri.app.shared.resources.about_tagline
import com.sumizuri.app.shared.resources.about_website
import com.sumizuri.app.shared.resources.about_website_hint
import com.sumizuri.app.shared.resources.app_name
import com.sumizuri.app.shared.resources.appupdate_new_version
import com.sumizuri.app.shared.resources.appupdate_title
import com.sumizuri.app.shared.resources.appupdate_title_hint
import com.sumizuri.app.shared.resources.legal_hint
import com.sumizuri.app.shared.resources.legal_title
import com.sumizuri.app.shared.resources.onboarding_replay
import com.sumizuri.app.shared.resources.onboarding_replay_hint
import com.sumizuri.app.shared.resources.privacy_hint
import com.sumizuri.app.shared.resources.privacy_title
import com.sumizuri.app.shared.resources.settings_about
import com.sumizuri.app.shared.resources.settings_version
import com.sumizuri.app.ui.kit.card.ListChevron
import com.sumizuri.app.ui.kit.setting.SettingIcon
import com.sumizuri.app.ui.kit.setting.SettingRow
import com.sumizuri.app.ui.kit.setting.SettingsSection
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

// The page about the app: its name and version, a way to its new versions, the first steps shown again, a way to the libraries it is built with, and the legal and privacy pages.
@Composable
fun AboutRoute(onBack: () -> Unit, onOpen: (SettingsTarget) -> Unit) {
    val container = LocalAppContainer.current
    val update by container.updater.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    // These links leave the app for the browser of the system, and never open in the browser inside the app.
    val uri = LocalUriHandler.current
    SettingsPageFrame(Res.string.settings_about, onBack, subtitle = Res.string.about_tagline) {
        SettingsSection {
            row {
                SettingRow(
                    stringResource(Res.string.app_name),
                    leading = {
                        SettingIcon(Icons.Outlined.Info)
                    },
                    subtitle = "${stringResource(
                        Res.string.settings_version,
                        BuildInfo.Version,
                    )} · ${stringResource(Res.string.about_tagline)}",
                )
            }
            row {
                SettingRow(
                    stringResource(Res.string.appupdate_title),
                    leading = { SettingIcon(Icons.Outlined.SystemUpdate) },
                    subtitle =
                    (update as? UpdateState.Available)?.let {
                        stringResource(
                            Res.string.appupdate_new_version,
                            it.latest.version,
                        )
                    }
                        ?: stringResource(Res.string.appupdate_title_hint),
                    trailing = { ListChevron() },
                    onClick = { onOpen(SettingsTarget.AppUpdate) },
                )
            }
            row {
                SettingRow(
                    stringResource(Res.string.onboarding_replay),
                    leading = { SettingIcon(Icons.Outlined.WavingHand) },
                    subtitle = stringResource(Res.string.onboarding_replay_hint),
                    trailing = { ListChevron() },
                    // The first steps show again over the app as soon as they are marked not done.
                    onClick = { scope.launch { container.settings.setFlag(SettingKey.OnboardingDone, false) } },
                )
            }
            row {
                SettingRow(
                    stringResource(Res.string.about_licenses),
                    leading = { SettingIcon(Icons.Outlined.Gavel) },
                    subtitle = stringResource(Res.string.about_licenses_hint),
                    trailing = { ListChevron() },
                    onClick = { onOpen(SettingsTarget.Licenses) },
                )
            }
            row {
                SettingRow(
                    stringResource(Res.string.legal_title),
                    leading = { SettingIcon(Icons.Outlined.Policy) },
                    subtitle = stringResource(Res.string.legal_hint),
                    trailing = { ListChevron() },
                    onClick = { onOpen(SettingsTarget.Legal) },
                )
            }
            row {
                SettingRow(
                    stringResource(Res.string.privacy_title),
                    leading = { SettingIcon(Icons.Outlined.PrivacyTip) },
                    subtitle = stringResource(Res.string.privacy_hint),
                    trailing = { ListChevron() },
                    onClick = { onOpen(SettingsTarget.Privacy) },
                )
            }
        }
        SettingsSection(Res.string.about_community) {
            for (link in Links) {
                row {
                    SettingRow(
                        stringResource(link.title),
                        leading = { SettingIcon(link.icon) },
                        subtitle = stringResource(link.hint),
                        trailing = { ListChevron() },
                        onClick = { runCatching { uri.openUri(link.address) } },
                    )
                }
            }
        }
    }
}

// Where to find the project and its people. Each opens in the browser.
private class CommunityLink(
    val title: StringResource,
    val hint: StringResource,
    val icon: ImageVector,
    val address: String,
)

private val Links = listOf(
    CommunityLink(
        Res.string.about_website,
        Res.string.about_website_hint,
        Icons.Outlined.Public,
        "https://sumizuri.notakari.com",
    ),
    CommunityLink(
        Res.string.about_discord,
        Res.string.about_discord_hint,
        Icons.Outlined.Forum,
        "https://discord.gg/JZaqv5VFj7",
    ),
    CommunityLink(
        Res.string.about_github,
        Res.string.about_github_hint,
        Icons.Outlined.Code,
        "https://github.com/not-akari/sumizuri",
    ),
    CommunityLink(
        Res.string.about_issues,
        Res.string.about_issues_hint,
        Icons.Outlined.BugReport,
        "https://github.com/not-akari/sumizuri/issues",
    ),
)
