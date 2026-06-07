package io.github.naharaoss.skpd.settings

import android.content.Context
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.naharaoss.skpd.utils.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val factory: SettingsFactoryDataSource,
    private val local: SettingsLocalDataSource,
    @param:ApplicationScope private val scope: CoroutineScope,
) {
    private val _settings: MutableStateFlow<AppSettings> = MutableStateFlow(AppSettings())
    private var _initialized = false
    val settings = _settings.asStateFlow()
    val initialized get() = _initialized

    init {
        scope.launch {
            val settings = local.readSettings()

            if (settings == null) {
                val factorySettings = factory.readFactorySettings()
                local.writeSettings(factorySettings)
                _settings.value = factorySettings
            } else {
                _settings.value = settings
            }

            _initialized = true
        }
    }

    suspend fun updateSettings(updater: (AppSettings) -> AppSettings) {
        _settings.update(updater)
        local.writeSettings(updater(local.readSettings() ?: _settings.value))
    }

    suspend fun updateSettings(settings: AppSettings) {
        _settings.value = settings
        local.writeSettings(settings)
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Singleton
class SettingsFactoryDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    suspend fun readFactorySettings(): AppSettings {
        val deviceBuiltin = "devices/${Build.MANUFACTURER}/${Build.DEVICE}"

        return withContext(Dispatchers.IO) {
            if (context.assets.list(deviceBuiltin)?.contains("settings.json") ?: false) {
                Log.i("Settings", "Found settings overlay: $deviceBuiltin")
                context.assets.open("$deviceBuiltin/settings.json").use { Json.decodeFromStream(it) }
            } else {
                Log.i("Settings", "No settings overlay: $deviceBuiltin")
                context.assets.open("factory/settings.json").use { Json.decodeFromStream(it) }
            }
        }
    }
}

@Singleton
class SettingsLocalDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    val file = File(context.filesDir, "settings.json")

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun writeSettings(settings: AppSettings) {
        withContext(Dispatchers.IO) {
            file.outputStream().use { Json.encodeToStream(settings, it) }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun readSettings(): AppSettings? {
        return withContext(Dispatchers.IO) {
            if (file.exists()) file.inputStream().use { Json.decodeFromStream(it) } else null
        }
    }
}
