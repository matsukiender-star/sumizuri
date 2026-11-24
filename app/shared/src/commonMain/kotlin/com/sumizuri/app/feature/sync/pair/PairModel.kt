package com.sumizuri.app.feature.sync.pair

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sumizuri.app.core.log.AppLog
import com.sumizuri.app.data.account.LibrarySyncer
import com.sumizuri.app.data.account.PairLink
import com.sumizuri.app.data.account.SyncClient
import com.sumizuri.app.data.account.SyncProblem
import com.sumizuri.app.data.account.thisDevice
import com.sumizuri.app.data.homeserver.HomePairing
import com.sumizuri.app.data.homeserver.homeHostOf
import com.sumizuri.app.data.homeserver.linkComputer
import com.sumizuri.app.data.homeserver.linkPhone
import com.sumizuri.app.data.profile.ProfileHost
import com.sumizuri.app.data.settings.SettingKey
import com.sumizuri.app.data.settings.SettingsRepository
import com.sumizuri.app.data.text.nowMillis
import com.sumizuri.app.navigation.AppLinks
import com.sumizuri.sync.core.Pairing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// The code being shown with until when it works, and how showing one or taking one goes.
internal data class PairUi(
    val code: String? = null,
    val until: Long = 0,
    val showing: Boolean = false,
    val showProblem: SyncProblem? = null,
    val joining: Boolean = false,
    val joinProblem: SyncProblem? = null,
)

// Pairing lives here and not in the page, which is built again as soon as the account is kept and would cancel it halfway.
internal class PairModel(
    private val profiles: ProfileHost,
    private val settings: SettingsRepository,
    private val syncer: LibrarySyncer,
    private val client: SyncClient = SyncClient(),
) : ViewModel() {
    private val store = profiles.syncAccounts
    private val pairing = profiles.registry.syncFile.parent?.let { folder ->
        homeHostOf(folder)?.let { HomePairing(folder, store, it) }
    }
    private val mutable = MutableStateFlow(PairUi())
    val ui: StateFlow<PairUi> = mutable

    // Only a computer that can be the server shows codes.
    val canShow: Boolean = pairing != null

    fun show() {
        val host = pairing ?: return
        if (mutable.value.showing) return
        mutable.update { it.copy(code = null, showing = true, showProblem = null) }
        viewModelScope.launch {
            val problem = pairStep {
                val (account, link) = host.invite()
                val until = nowMillis() + Pairing.OfferMs
                if (linkComputer(client, settings, account, profileName())) syncer.syncSoon()
                mutable.update { it.copy(code = link.text(), until = until) }
            }
            mutable.update { it.copy(showing = false, showProblem = problem) }
        }
    }

    fun join(link: PairLink) {
        val before = store.account.value
        if (mutable.value.joining || !link.fits(before)) return
        mutable.update { it.copy(joining = true, joinProblem = null) }
        viewModelScope.launch {
            val problem = pairStep {
                val joined = client.joinPairing(link, thisDevice())
                // Kept at once, since the code is used up now whatever happens next.
                store.save(joined)
                // Scanned again it keeps the profile it had, and only the address is new.
                val again = before?.userId == joined.userId && settings.textNow(SettingKey.SyncLibraryId) != null
                if (again || linkPhone(client, settings, joined, profileName())) syncer.syncSoon()
                AppLog.info("sync", "linked with a computer by its code")
                AppLinks.homePairTaken()
            }
            mutable.update { it.copy(joining = false, joinProblem = problem) }
        }
    }

    fun dismissJoin() {
        mutable.update { it.copy(joinProblem = null) }
        AppLinks.homePairTaken()
    }

    private fun profileName(): String = profiles.registry.state.value.active.name
}

// Runs a step of pairing, giving what went wrong in the shape the page shows.
private suspend fun pairStep(work: suspend () -> Unit): SyncProblem? = try {
    work()
    null
} catch (cancel: CancellationException) {
    throw cancel
} catch (problem: SyncProblem) {
    problem
} catch (error: Exception) {
    SyncProblem(SyncProblem.BadAnswer, error.message ?: error::class.simpleName.orEmpty())
}
