package com.sumizuri.app.data.settings

import com.sumizuri.app.data.db.AppDatabase
import com.sumizuri.app.data.db.setting.SettingEntity
import com.sumizuri.app.data.model.controls.AppGestures
import com.sumizuri.app.data.model.controls.ReaderControls
import com.sumizuri.app.data.model.controls.appGesturesOf
import com.sumizuri.app.data.model.controls.readerControlsOf
import com.sumizuri.app.data.model.controls.toJson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private const val ReaderKey = "controls.reader"
private const val AppKey = "controls.app"

// The keys, the taps and the gestures the person set. They are kept as one text each, and a setting that was never changed is not stored at all.
class ControlsRepository(private val db: AppDatabase) {
    val reader: Flow<ReaderControls> = db
        .settings()
        .observe(ReaderKey)
        .distinctUntilChanged()
        .map(::readerControlsOf)

    val app: Flow<AppGestures> = db
        .settings()
        .observe(AppKey)
        .distinctUntilChanged()
        .map(::appGesturesOf)

    suspend fun setReader(controls: ReaderControls) = if (controls.isDefault) {
        db.settings().delete(
            ReaderKey,
        )
    } else {
        db.settings().put(SettingEntity(ReaderKey, controls.toJson()))
    }

    suspend fun setApp(gestures: AppGestures) = if (gestures.isDefault) {
        db.settings().delete(
            AppKey,
        )
    } else {
        db.settings().put(SettingEntity(AppKey, gestures.toJson()))
    }
}
