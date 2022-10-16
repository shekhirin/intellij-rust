/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.configurable

import com.intellij.execution.configuration.EnvironmentVariablesComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.EnumComboBoxModel
import com.intellij.ui.components.Label
import com.intellij.ui.dsl.builder.*
import org.rust.RsBundle
import org.rust.cargo.project.model.cargoProjects
import org.rust.cargo.project.settings.RsExternalLinterProjectSettingsService
import org.rust.cargo.project.settings.externalLinterSettings
import org.rust.cargo.toolchain.ExternalLinter
import org.rust.cargo.toolchain.RustChannel
import org.rust.cargo.util.CargoCommandCompletionProvider
import org.rust.cargo.util.RsCommandLineEditor
import org.rust.openapiext.fullWidthCell
import javax.swing.JLabel

class RsExternalLinterConfigurable(project: Project) : RsConfigurableBase(project, RsBundle.message("settings.rust.external.linters.name")) {
    private val settings: RsExternalLinterProjectSettingsService = project.externalLinterSettings

    private val additionalArguments: RsCommandLineEditor =
        RsCommandLineEditor(project, CargoCommandCompletionProvider(project.cargoProjects, "check ") { null })

    private val channelLabel: JLabel = Label(RsBundle.message("settings.rust.external.linters.channel.label"))
    private val channel: ComboBox<RustChannel> = ComboBox<RustChannel>().apply {
        RustChannel.values()
            .sortedBy { it.index }
            .forEach { addItem(it) }
    }

    private val environmentVariables: EnvironmentVariablesComponent = EnvironmentVariablesComponent()

    override fun createPanel(): DialogPanel = panel {
        group(indent = false) {
            row(RsBundle.message("settings.rust.external.linters.tool.label")) {
                comboBox(EnumComboBoxModel(ExternalLinter::class.java))
                    .comment(RsBundle.message("settings.rust.external.linters.tool.comment"))
                    .bindItem(settings.state::tool.toNullableProperty())
            }

            row(RsBundle.message("settings.rust.external.linters.additional.arguments.label")) {
                fullWidthCell(additionalArguments)
                    .resizableColumn()
                    .comment(RsBundle.message("settings.rust.external.linters.additional.arguments.comment"))
                    .bind(
                        componentGet = { it.text },
                        componentSet = { component, value -> component.text = value },
                        prop = settings.state::additionalArguments.toMutableProperty()
                    )

                channelLabel.labelFor = channel
                cell(channelLabel)
                cell(channel)
                    .bind(
                        componentGet = { it.item },
                        componentSet = { component, value -> component.item = value },
                        prop = settings.state::channel.toMutableProperty()
                    )
            }

            row(environmentVariables.label) {
                fullWidthCell(environmentVariables)
                    .bind(
                        componentGet = { it.envs },
                        componentSet = { component, value -> component.envs = value },
                        prop = settings.state::envs.toMutableProperty()
                    )
            }
        }.bottomGap(BottomGap.MEDIUM) // TODO: do we really need it?

        row {
            checkBox(RsBundle.message("settings.rust.external.linters.on.the.fly.label"))
                .comment(RsBundle.message("settings.rust.external.linters.on.the.fly.comment"))
                .bindSelected(settings.state::runOnTheFly)
        }
    }
}
