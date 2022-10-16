/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.settings

import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.project.Project
import org.rust.cargo.project.settings.RsExternalLinterProjectSettingsService.State
import org.rust.cargo.toolchain.ExternalLinter

val Project.externalLinterSettings: RsExternalLinterProjectSettingsService
    get() = getService(RsExternalLinterProjectSettingsService::class.java)
        ?: error("Failed to get ${serviceName}Service for $this")

private const val serviceName: String = "RsExternalLinterProjectSettings"

@com.intellij.openapi.components.State(name = serviceName, storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class RsExternalLinterProjectSettingsService(project: Project) : RsProjectSettingsServiceBase<State>(project, State()) {
    val tool: ExternalLinter get() = state.tool
    val additionalArguments: String get() = state.additionalArguments
    val runOnTheFly: Boolean get() = state.runOnTheFly

    override fun noStateLoaded() {
        val rustSettings = project.rustSettings
        state.tool = rustSettings.state.externalLinter
        rustSettings.state.externalLinter = ExternalLinter.DEFAULT
        state.additionalArguments = rustSettings.state.externalLinterArguments
        rustSettings.state.externalLinterArguments = ""
        state.runOnTheFly = rustSettings.state.runExternalLinterOnTheFly
        rustSettings.state.runExternalLinterOnTheFly = false
    }

    class State : StateBase<State>() {
        @AffectsHighlighting
        var tool by enum(ExternalLinter.DEFAULT)
        @AffectsHighlighting
        var additionalArguments by property("") { it.isEmpty() }
        @AffectsHighlighting
        var runOnTheFly by property(false)

        override fun copy(): State {
            val state = State()
            state.copyFrom(this)
            return state
        }
    }

    override fun createSettingsChangedEvent(oldEvent: State, newEvent: State): SettingsChangedEvent {
        return SettingsChangedEvent(oldEvent, newEvent)
    }

    class SettingsChangedEvent(oldState: State, newState: State) : SettingsChangedEventBase<State>(oldState, newState)
}
