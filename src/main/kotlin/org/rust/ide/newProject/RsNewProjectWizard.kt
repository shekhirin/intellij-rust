/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.newProject

import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.LanguageNewProjectWizard
import com.intellij.ide.wizard.NewProjectWizardLanguageStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import org.rust.ide.module.RsModuleBuilder
import com.intellij.ide.wizard.GitNewProjectWizardData.Companion.gitData
import org.rust.stdext.toPathOrNull
import java.nio.file.Path
import java.nio.file.Paths

class RsNewProjectWizard : LanguageNewProjectWizard {
    override val name: String = "Rust" // TODO: replace with `NewProjectWizardConstants.Language.RUST`

    override val ordinal: Int = 1000

    override fun createStep(parent: NewProjectWizardLanguageStep): NewProjectWizardStep = Step(parent)

    private class Step(val parent: NewProjectWizardLanguageStep) : AbstractNewProjectWizardStep(parent) {
        private val peer: RsProjectGeneratorPeer = RsProjectGeneratorPeer(parent.path.toPathOrNull() ?: Paths.get("."))

        override fun setupUI(builder: Panel) {
            with(builder) {
                row {
                    cell(peer.component)
                        .horizontalAlign(HorizontalAlign.FILL)
                        .validationRequestor { peer.checkValid = Runnable(it) }
                        .validation { peer.validate() }
                }
            }
        }

        override fun setupProject(project: Project) {
            val builder = RsModuleBuilder()
            val module = builder.commit(project).firstOrNull() ?: return
            ModuleRootModificationUtil.updateModel(module) { rootModel ->
                builder.configurationData = peer.settings
                builder.createProject(
                    rootModel,
                    vcs = if (gitData?.git == true) "git" else "none"
                )
            }
        }
    }
}
