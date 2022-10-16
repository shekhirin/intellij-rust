/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project

import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.util.xmlb.XmlSerializer
import org.intellij.lang.annotations.Language
import org.rust.cargo.project.settings.RustfmtProjectSettingsService
import org.rust.cargo.toolchain.RustChannel
import org.rust.openapiext.elementFromXmlString
import org.rust.openapiext.toXmlString

class RustfmtProjectSettingsServiceTest : LightPlatformTestCase() {
    fun `test serialization`() {
        val service = RustfmtProjectSettingsService(project)

        @Language("XML")
        val text = """
            <State>
              <option name="additionalArguments" value="--unstable-features" />
              <option name="channel" value="nightly" />
              <option name="envs">
                <map>
                  <entry key="ABC" value="123" />
                </map>
              </option>
              <option name="runRustfmtOnSave" value="true" />
              <option name="useRustfmt" value="true" />
            </State>
        """.trimIndent()
        service.loadState(stateFromXmlString(text))

        val actual = XmlSerializer.serialize(service.state).toXmlString()
        assertEquals(text, actual)

        assertEquals("--unstable-features", service.additionalArguments)
        assertEquals(RustChannel.NIGHTLY, service.channel)
        assertEquals(mapOf("ABC" to "123"), service.envs)
        assertEquals(true, service.useRustfmt)
        assertEquals(true, service.runRustfmtOnSave)
    }

    companion object {
        private fun stateFromXmlString(xml: String): RustfmtProjectSettingsService.State {
            val element = elementFromXmlString(xml)
            return XmlSerializer.deserialize(element, RustfmtProjectSettingsService.State::class.java)
        }
    }
}
