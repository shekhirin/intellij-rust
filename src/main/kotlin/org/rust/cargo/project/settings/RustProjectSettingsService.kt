/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.settings

import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.util.ThreeState
import com.intellij.util.io.systemIndependentPath
import com.intellij.util.xmlb.annotations.Transient
import org.rust.cargo.project.configurable.RsProjectConfigurable
import org.rust.cargo.project.settings.RustProjectSettingsService.State
import org.rust.cargo.toolchain.ExternalLinter
import org.rust.cargo.toolchain.RsToolchain
import org.rust.cargo.toolchain.RsToolchainBase
import org.rust.cargo.toolchain.RsToolchainProvider
import org.rust.openapiext.isUnitTestMode
import org.rust.openapiext.showSettingsDialog
import java.nio.file.Paths

val Project.rustSettings: RustProjectSettingsService
    get() = getService(RustProjectSettingsService::class.java)
        ?: error("Failed to get ${serviceName}Service for $this")

val Project.toolchain: RsToolchainBase?
    get() {
        val toolchain = rustSettings.state.toolchain
        return when {
            toolchain != null -> toolchain
            isUnitTestMode -> RsToolchainBase.suggest()
            else -> null
        }
    }

private const val serviceName: String = "RustProjectSettings"

@com.intellij.openapi.components.State(name = serviceName, storages = [
    Storage(StoragePathMacros.WORKSPACE_FILE),
    Storage("misc.xml", deprecated = true)
])
class RustProjectSettingsService(project: Project) : RsProjectSettingsServiceBase<State>(project, State()) {
    val toolchain: RsToolchainBase? get() = state.toolchain
    val explicitPathToStdlib: String? get() = state.explicitPathToStdlib
    val autoShowErrorsInEditor: ThreeState get() = ThreeState.fromBoolean(state.autoShowErrorsInEditor)
    val autoUpdateEnabled: Boolean get() = state.autoUpdateEnabled
    val compileAllTargets: Boolean get() = state.compileAllTargets
    val useOffline: Boolean get() = state.useOffline
    val macroExpansionEngine: MacroExpansionEngine get() = state.macroExpansionEngine
    val doctestInjectionEnabled: Boolean get() = state.doctestInjectionEnabled

    class State : StateBase<State>() {
        @AffectsCargoMetadata
        var toolchainHomeDirectory by string()
        var autoShowErrorsInEditor by property(true)
        var autoUpdateEnabled by property(true)
        // Usually, we use `rustup` to find stdlib automatically,
        // but if one does not use rustup, it's possible to
        // provide path to stdlib explicitly.
        @AffectsCargoMetadata
        var explicitPathToStdlib by string()
        // BACKCOMPAT: 2022.2
        var externalLinter by enum(ExternalLinter.DEFAULT)
        // BACKCOMPAT: 2022.2
        var runExternalLinterOnTheFly by property(false)
        // BACKCOMPAT: 2022.2
        var externalLinterArguments by property("") { it.isEmpty() }
        @AffectsHighlighting
        var compileAllTargets by property(true)
        var useOffline by property(false)
        var macroExpansionEngine by enum(defaultMacroExpansionEngine)
        @AffectsHighlighting
        var doctestInjectionEnabled by property(true)

        @get:Transient
        @set:Transient
        var toolchain: RsToolchainBase?
            get() = toolchainHomeDirectory?.let { RsToolchainProvider.getToolchain(Paths.get(it)) }
            set(value) {
                toolchainHomeDirectory = value?.location?.systemIndependentPath
            }

        @Suppress("DEPRECATION", "DeprecatedCallableAddReplaceWith")
        @Deprecated("Use toolchain property")
        fun setToolchain(toolchain: RsToolchain?) {
            toolchainHomeDirectory = toolchain?.location?.systemIndependentPath
        }

        override fun copy(): State {
            val state = State()
            state.copyFrom(this)
            return state
        }
    }

    override fun loadState(state: State) {
        if (state.macroExpansionEngine == MacroExpansionEngine.OLD) {
            state.macroExpansionEngine = MacroExpansionEngine.NEW
        }
        super.loadState(state)
    }

    override fun notifySettingsChanged(event: SettingsChangedEventBase<State>) {
        super.notifySettingsChanged(event)

        if (event.isChanged(State::doctestInjectionEnabled)) {
            // flush injection cache
            PsiManager.getInstance(project).dropPsiCaches()
        }
    }

    override fun createSettingsChangedEvent(oldEvent: State, newEvent: State): SettingsChangedEvent {
        return SettingsChangedEvent(oldEvent, newEvent)
    }

    class SettingsChangedEvent(oldState: State, newState: State) : SettingsChangedEventBase<State>(oldState, newState)

    @Suppress("DEPRECATION")
    @Deprecated("Use toolchain property")
    fun getToolchain(): RsToolchain? = state.toolchain?.let(RsToolchain::from)

    /**
     * Show a dialog for toolchain configuration
     */
    fun configureToolchain() {
        project.showSettingsDialog<RsProjectConfigurable>()
    }

    enum class MacroExpansionEngine {
        DISABLED,
        OLD, // `OLD` can't be selected by a user anymore, it exists for backcompat with saved user settings
        NEW
    }

    companion object {
        private val defaultMacroExpansionEngine: MacroExpansionEngine
            get() = MacroExpansionEngine.NEW
    }
}
