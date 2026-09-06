package com.minima.launcher.data

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.minima.launcher.ai.AIAssistant
import com.minima.launcher.ai.AIManager
import com.minima.launcher.ai.AssistantRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LauncherViewModel(
    private val repo: AppRepository,
    val ai: AIManager,
) : ViewModel() {

    private val _apps = MutableStateFlow<List<AppModel>>(emptyList())
    val apps: StateFlow<List<AppModel>> = _apps.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _defaultAi = MutableStateFlow<AIAssistant>(AssistantRegistry.all.first())
    val defaultAi: StateFlow<AIAssistant> = _defaultAi.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            ai.defaultAssistant.collect { _defaultAi.value = it }
        }
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _apps.value = repo.loadApps()
        }
    }

    fun onQuery(q: String) { _query.value = q }

    fun filteredApps(): List<AppModel> {
        val q = _query.value.trim().lowercase()
        if (q.isEmpty()) return _apps.value
        return _apps.value.filter {
            it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
        }
    }

    fun launch(app: AppModel) = repo.launch(app)

    fun askAi(prompt: String) {
        if (prompt.isBlank()) return
        // If query matches no app, it is likely an AI question — route it.
        ai.ask(prompt, _defaultAi.value)
    }

    fun setDefaultAi(id: String) {
        viewModelScope.launch { ai.setDefault(id) }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val appCtx = context.applicationContext
                    return LauncherViewModel(AppRepository(appCtx), AIManager(appCtx)) as T
                }
            }
    }
}
