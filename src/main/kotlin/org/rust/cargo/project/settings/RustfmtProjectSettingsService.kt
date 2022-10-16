/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.settings

import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.project.Project
import org.rust.cargo.project.settings.RustfmtProjectSettingsService.State
import org.rust.cargo.toolchain.RustChannel

val Project.rustfmtSettings: RustfmtProjectSettingsService
    get() = getService(RustfmtProjectSettingsService::class.java)
        ?: error("Failed to get ${serviceName}Service for $this")

private const val serviceName: String = "RustfmtProjectSettings"

@com.intellij.openapi.components.State(name = serviceName, storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class RustfmtProjectSettingsService(project: Project) : RsProjectSettingsServiceBase<State>(project, State()) {
    val additionalArguments: String get() = state.additionalArguments
    val channel: RustChannel get() = state.channel
    val envs: Map<String, String> get() = state.envs
    val useRustfmt: Boolean get() = state.useRustfmt
    val runRustfmtOnSave: Boolean get() = state.runRustfmtOnSave

    class State : StateBase<State>() {
        var additionalArguments by property("") { it.isEmpty() }
        var channel by enum(RustChannel.DEFAULT)
        var envs by map<String, String>()
        var useRustfmt by property(false)
        var runRustfmtOnSave by property(false)

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
