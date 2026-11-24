package com.sumizuri.app.feature.sync

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sumizuri.app.data.LocalAppContainer
import com.sumizuri.app.data.account.SyncClient
import com.sumizuri.app.data.net.rememberLocalNetworkAccess
import com.sumizuri.app.data.profile.LocalProfiles
import com.sumizuri.app.feature.settings.SettingsPageFrame
import com.sumizuri.app.feature.settings.SettingsTarget
import com.sumizuri.app.feature.sync.home.SyncHomeView
import com.sumizuri.app.feature.sync.home.SyncPageWidth
import com.sumizuri.app.feature.sync.pair.PairJoinDialog
import com.sumizuri.app.feature.sync.pair.PairModel
import com.sumizuri.app.feature.sync.pair.PairPhoneSection
import com.sumizuri.app.feature.sync.signin.AccountChoice
import com.sumizuri.app.feature.sync.signin.AddressSection
import com.sumizuri.app.feature.sync.signin.ChooseProfileSection
import com.sumizuri.app.feature.sync.signin.CreateSection
import com.sumizuri.app.feature.sync.signin.RecoverSection
import com.sumizuri.app.feature.sync.signin.RecoverySection
import com.sumizuri.app.feature.sync.signin.ServerSection
import com.sumizuri.app.feature.sync.signin.SetupSection
import com.sumizuri.app.feature.sync.signin.SetupStep
import com.sumizuri.app.feature.sync.signin.SignInSection
import com.sumizuri.app.feature.sync.signin.StepBar
import com.sumizuri.app.feature.sync.signin.SyncMode
import com.sumizuri.app.feature.sync.signin.SyncStep
import com.sumizuri.app.feature.sync.signin.SyncViewModel
import com.sumizuri.app.feature.sync.signin.WelcomeCard
import com.sumizuri.app.feature.sync.signin.setupStep
import com.sumizuri.app.navigation.AppLinks
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.sync_hint
import com.sumizuri.app.shared.resources.sync_title
import com.sumizuri.app.ui.kit.page.ReadableWidth

// The sync page: the address of a server, then setting it up or signing in, the recovery key once, and the account.
@Composable
fun SyncRoute(onBack: () -> Unit, onOpen: (SettingsTarget) -> Unit) {
    val profiles = LocalProfiles.current
    val container = LocalAppContainer.current
    val profileName = remember(profiles) { profiles.registry.state.value.active.name }
    val model = viewModel { SyncViewModel(profiles.syncAccounts, container.settings, profileName) }
    val ui by model.ui.collectAsStateWithLifecycle()
    val pair = viewModel { PairModel(profiles, container.settings, container.librarySync) }
    // Signed in, the page uses more of a wide window for its tiles.
    val connected = ui.step as? SyncStep.Connected
    val askHomeNetwork = rememberLocalNetworkAccess()
    // An account kept from before the system began to ask is asked for when its page opens.
    val homeServer = connected?.account?.server?.takeIf(SyncClient::leadsHome)
    LaunchedEffect(homeServer) { if (homeServer != null) askHomeNetwork {} }
    PairJoinDialog(pair)
    SettingsPageFrame(
        Res.string.sync_title,
        onBack,
        subtitle = Res.string.sync_hint,
        maxWidth = if (connected != null) SyncPageWidth else ReadableWidth,
    ) {
        // Until sync is going, a step bar says where the person is, with what sync does over the first step.
        ui.step.setupStep()?.let { now ->
            if (now == SetupStep.Server) WelcomeCard()
            StepBar(now)
        }
        // Linking this phone to a computer at home with its code, or showing the code on a computer, before a server is typed and once sync is going.
        val here = ui.step
        if (here is SyncStep.Address || here is SyncStep.Connected) PairPhoneSection(pair)
        when (val step = ui.step) {
            SyncStep.Address -> AddressSection(ui.busy) { typed ->
                if (AppLinks.open(typed)) return@AddressSection
                if (SyncClient.leadsHome(typed)) askHomeNetwork { model.connect(typed) } else model.connect(typed)
            }

            is SyncStep.Server -> {
                ServerSection(step.address, step.info, ui.busy, model::back)
                AccountChoice(step.mode, step.info.signupMode, model::choose)
                when (step.mode) {
                    SyncMode.Setup -> SetupSection(step.info.minPassword, ui.busy, model::setup)
                    SyncMode.SignIn -> SignInSection(step.info, step.askCode, ui.busy, model::signIn, model::choose)
                    SyncMode.Create -> CreateSection(step.info, ui.busy, model::create, model::choose)
                    SyncMode.Recover -> RecoverSection(step.info.minPassword, ui.busy, model::recover, model::choose)
                }
            }

            is SyncStep.SaveRecovery -> RecoverySection(step, model::savedRecovery)

            is SyncStep.ChooseProfile -> ChooseProfileSection(
                step,
                ui.busy,
                model::pick,
                model::makeProfile,
                model::skipChoice,
            )

            is SyncStep.Connected -> SyncHomeView(step.account, onOpen)
        }
        if (ui.busy) Working()
        ui.problem?.let { Problem(it) }
    }
}
