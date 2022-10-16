/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project

import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.util.xmlb.XmlSerializer
import org.intellij.lang.annotations.Language
import org.rust.cargo.project.settings.RustProjectSettingsService
import org.rust.cargo.project.settings.RustProjectSettingsService.MacroExpansionEngine
import org.rust.cargo.toolchain.RsLocalToolchain
import org.rust.openapiext.elementFromXmlString
import org.rust.openapiext.toXmlString
import java.nio.file.Paths

class RustProjectSettingsServiceTest : LightPlatformTestCase() {

    fun `test serialization`() {
        val service = RustProjectSettingsService(project)

        @Language("XML")
        val text = """
            <State>
              <option name="autoUpdateEnabled" value="false" />
              <option name="compileAllTargets" value="false" />
              <option name="doctestInjectionEnabled" value="false" />
              <option name="explicitPathToStdlib" value="/stdlib" />
              <option name="externalLinter" value="Clippy" />
              <option name="externalLinterArguments" value="--no-default-features" />
              <option name="macroExpansionEngine" value="DISABLED" />
              <option name="runExternalLinterOnTheFly" value="true" />
              <option name="toolchainHomeDirectory" value="/" />
              <option name="useOffline" value="true" />
            </State>
        """.trimIndent()
        service.loadState(stateFromXmlString(text))

        val actual = XmlSerializer.serialize(service.state).toXmlString()
        assertEquals(text, actual)

        assertEquals(RsLocalToolchain(Paths.get("/")), service.toolchain)
        assertEquals(false, service.autoUpdateEnabled)
        assertEquals("/stdlib", service.explicitPathToStdlib)
        assertEquals(false, service.compileAllTargets)
        assertEquals(true, service.useOffline)
        assertEquals(MacroExpansionEngine.DISABLED, service.macroExpansionEngine)
        assertEquals(false, service.doctestInjectionEnabled)
    }

    fun `test serialization with old fields`() {
        val service = RustProjectSettingsService(project)

        @Language("XML")
        val text = """
            <State>
              <option name="autoUpdateEnabled" value="false" />
              <option name="compileAllTargets" value="false" />
              <option name="doctestInjectionEnabled" value="false" />
              <option name="explicitPathToStdlib" value="/stdlib" />
              <option name="externalLinter" value="Clippy" />
              <option name="externalLinterArguments" value="--no-default-features" />
              <option name="macroExpansionEngine" value="DISABLED" />
              <option name="runExternalLinterOnTheFly" value="true" />
              <option name="runRustfmtOnSave" value="true" /> <!-- Old field -->
              <option name="toolchainHomeDirectory" value="/" />
              <option name="useOffline" value="true" />
              <option name="useRustfmt" value="true" /> <!-- Old field -->
            </State>
        """.trimIndent()
        service.loadState(stateFromXmlString(text))

        val actual = XmlSerializer.serialize(service.state).toXmlString()
        @Language("XML")
        val expected = """
            <State>
              <option name="autoUpdateEnabled" value="false" />
              <option name="compileAllTargets" value="false" />
              <option name="doctestInjectionEnabled" value="false" />
              <option name="explicitPathToStdlib" value="/stdlib" />
              <option name="externalLinter" value="Clippy" />
              <option name="externalLinterArguments" value="--no-default-features" />
              <option name="macroExpansionEngine" value="DISABLED" />
              <option name="runExternalLinterOnTheFly" value="true" />
              <option name="toolchainHomeDirectory" value="/" />
              <option name="useOffline" value="true" />
            </State>
        """.trimIndent()

        assertEquals(expected, actual)
    }

    companion object {
        private fun stateFromXmlString(xml: String): RustProjectSettingsService.State {
            val element = elementFromXmlString(xml)
            return XmlSerializer.deserialize(element, RustProjectSettingsService.State::class.java)
        }
    }
}
