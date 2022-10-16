/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project

import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.util.xmlb.XmlSerializer
import org.intellij.lang.annotations.Language
import org.junit.internal.runners.JUnit38ClassRunner
import org.junit.runner.RunWith
import org.rust.cargo.project.settings.RsExternalLinterProjectSettingsService
import org.rust.cargo.toolchain.ExternalLinter
import org.rust.openapiext.elementFromXmlString
import org.rust.openapiext.toXmlString

@RunWith(JUnit38ClassRunner::class) // TODO: drop the annotation when issue with Gradle test scanning go away
class RsExternalLinterProjectSettingsServiceTest : LightPlatformTestCase() {

    fun `test serialization`() {
        val service = RsExternalLinterProjectSettingsService(project)

        @Language("XML")
        val text = """
            <State>
              <option name="additionalArguments" value="--unstable-features" />
              <option name="runOnTheFly" value="true" />
              <option name="tool" value="Clippy" />
            </State>
        """.trimIndent()
        service.loadState(stateFromXmlString(text))

        val actual = XmlSerializer.serialize(service.state).toXmlString()
        assertEquals(text, actual)

        assertEquals("--unstable-features", service.additionalArguments)
        assertEquals(ExternalLinter.CLIPPY, service.tool)
        assertEquals(true, service.runOnTheFly)
    }

    companion object {
        private fun stateFromXmlString(xml: String): RsExternalLinterProjectSettingsService.State {
            val element = elementFromXmlString(xml)
            return XmlSerializer.deserialize(element, RsExternalLinterProjectSettingsService.State::class.java)
        }
    }
}
