/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain.impl

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import org.rust.cargo.project.workspace.PackageId
import org.rust.ide.annotator.isValid

// https://docs.rs/cargo/0.33.0/cargo/util/machine_message/struct.FromCompiler.html
data class CargoTopMessage(
    val message: RustcMessage,
    val package_id: String,
    val reason: String,
    val target: Target
) {
    companion object {
        private val LOG: Logger = logger<CargoTopMessage>()

        fun fromJson(json: JsonObject): CargoTopMessage? {
            if (json.getAsJsonPrimitive("reason")?.asString != "compiler-message") {
                return null
            }

            val message = try {
                Gson().fromJson(json, CargoTopMessage::class.java)
            } catch (e: JsonSyntaxException) {
                LOG.warn(e)
                null
            }
            return message ?: error("Failed to parse CargoTopMessage from $json")
        }
    }
}

// https://doc.rust-lang.org/nightly/nightly-rustc/syntax/json/struct.Diagnostic.html
data class RustcMessage(
    val children: List<RustcMessage>,
    val code: ErrorCode?,
    val level: String,
    val message: String,
    val rendered: String?,
    val spans: List<RustcSpan>
) {
    val mainSpan: RustcSpan?
        get() {
            val validSpan = spans.filter { it.isValid() }.firstOrNull { it.is_primary } ?: return null
            return generateSequence(validSpan) { it.expansion?.span }.last()
                .takeIf { it.isValid() && !it.file_name.startsWith("<") }
        }
}

// https://doc.rust-lang.org/nightly/nightly-rustc/syntax/json/struct.DiagnosticCode.html
data class ErrorCode(
    val code: String,
    val explanation: String?
)

// https://doc.rust-lang.org/nightly/nightly-rustc/syntax/json/struct.DiagnosticSpan.html
data class RustcSpan(
    val file_name: String,
    val byte_start: Int,
    val byte_end: Int,
    val line_start: Int,
    val line_end: Int,
    val column_start: Int,
    val column_end: Int,
    val is_primary: Boolean,
    val text: List<RustcText>,
    val label: String?,
    val suggested_replacement: String?,
    val suggestion_applicability: Applicability?,
    val expansion: Expansion?
) {
    fun toTextRange(document: Document): TextRange? {
        val startOffset = toOffset(document, line_start, column_start)
        val endOffset = toOffset(document, line_end, column_end)
        return if (startOffset != null && endOffset != null && startOffset < endOffset) {
            TextRange(startOffset, endOffset)
        } else {
            null
        }
    }

    companion object {
        @Suppress("NAME_SHADOWING")
        fun toOffset(document: Document, line: Int, column: Int): Int? {
            val line = line - 1
            val column = column - 1
            if (line < 0 || line >= document.lineCount) return null
            return (document.getLineStartOffset(line) + column)
                .takeIf { it <= document.textLength }
        }
    }
}

// https://doc.rust-lang.org/nightly/nightly-rustc/syntax/json/struct.DiagnosticSpanMacroExpansion.html
data class Expansion(
    val def_site_span: RustcSpan?,
    val macro_decl_name: String,
    val span: RustcSpan
)

// https://doc.rust-lang.org/nightly/nightly-rustc/syntax/json/struct.DiagnosticSpanLine.html
data class RustcText(
    val highlight_end: Int,
    val highlight_start: Int,
    val text: String?
)

// https://docs.rs/cargo/0.33.0/cargo/core/manifest/struct.Target.html
data class Target(
    val crate_types: List<String>,
    val kind: List<String>,
    val name: String,
    val src_path: String
)

// https://doc.rust-lang.org/nightly/nightly-rustc/syntax/diagnostics/plugin/enum.Applicability.html
@Suppress("unused")
enum class Applicability {
    @SerializedName("MachineApplicable")
    MACHINE_APPLICABLE,

    @SerializedName("MaybeIncorrect")
    MAYBE_INCORRECT,

    @SerializedName("HasPlaceholders")
    HAS_PLACEHOLDERS,

    @SerializedName("Unspecified")
    UNSPECIFIED
}

sealed class CompilerMessage {
    abstract val package_id: PackageId

    abstract fun convertPaths(converter: PathConverter): CompilerMessage

    companion object {
        fun fromJson(json: JsonObject): CompilerMessage? {
            val reason = json.getAsJsonPrimitive("reason")?.asString ?: return null
            val cls: Class<out CompilerMessage> = when (reason) {
                BuildScriptMessage.REASON -> BuildScriptMessage::class.java
                CompilerArtifactMessage.REASON -> CompilerArtifactMessage::class.java
                else -> return null
            }
            return Gson().fromJson(json, cls)
        }
    }
}

/**
 * Represents execution result of build script
 *
 * @see <a href="https://github.com/rust-lang/cargo/blob/f0f73f04d104b67f982c3e24f074f48308c0afd0/src/cargo/util/machine_message.rs#L62-L70">machine_message.rs</a>
 */
data class BuildScriptMessage(
    override val package_id: PackageId,
    val cfgs: List<String>,
    val env: List<List<String>>,
    val out_dir: String?
) : CompilerMessage() {

    override fun convertPaths(converter: PathConverter): BuildScriptMessage = copy(
        out_dir = out_dir?.let(converter)
    )

    companion object {
        const val REASON: String = "build-script-executed"
    }
}

/**
 * Represents some compiled artifact
 *
 * @see <a href="https://github.com/rust-lang/cargo/blob/f0f73f04d104b67f982c3e24f074f48308c0afd0/src/cargo/util/machine_message.rs#L33-L42">machine_message.rs</a>
 */
data class CompilerArtifactMessage(
    override val package_id: PackageId,
    val target: CargoMetadata.Target,
    val profile: Profile,
    val filenames: List<String>,
    val executable: String?
) : CompilerMessage() {

    val executables: List<String>
        get() {
            return if (executable != null) {
                listOf(executable)
            } else {
                /**
                 * `.dSYM` and `.pdb` files are binaries, but they should not be used when starting debug session.
                 * Without this filtering, CLion shows error message about several binaries
                 * in case of disabled build tool window
                 */
                // BACKCOMPAT: Cargo 0.34.0
                filenames.filter { !it.endsWith(".dSYM") && !it.endsWith(".pdb") }
            }
        }

    override fun convertPaths(converter: PathConverter): CompilerArtifactMessage = copy(
        target = target.convertPaths(converter),
        filenames = filenames.map(converter),
        executable = executable?.let(converter)
    )

    companion object {
        const val REASON: String = "compiler-artifact"

        fun fromJson(json: JsonObject): CompilerArtifactMessage? {
            if (json.getAsJsonPrimitive("reason").asString != REASON) {
                return null
            }
            return Gson().fromJson(json, CompilerArtifactMessage::class.java)
        }
    }
}

data class Profile(
    val test: Boolean
)
