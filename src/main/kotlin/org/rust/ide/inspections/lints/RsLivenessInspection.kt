/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.psi.PsiElement
import org.rust.ide.injected.isDoctestInjection
import org.rust.ide.inspections.RsProblemsHolder
import org.rust.ide.inspections.RsWithMacrosInspectionVisitor
import org.rust.ide.inspections.fixes.RemoveParameterFix
import org.rust.ide.inspections.fixes.RemoveVariableFix
import org.rust.ide.inspections.fixes.RenameFix
import org.rust.lang.core.dfa.liveness.DeclarationKind
import org.rust.lang.core.dfa.liveness.DeclarationKind.Parameter
import org.rust.lang.core.dfa.liveness.DeclarationKind.Variable
import org.rust.lang.core.psi.*
import org.rust.lang.core.psi.ext.*
import org.rust.lang.core.types.liveness

class RsLivenessInspection : RsLintInspection() {

    override fun getLint(element: PsiElement): RsLint = RsLint.UnusedVariables

    override fun buildVisitor(holder: RsProblemsHolder, isOnTheFly: Boolean): RsVisitor =
        object : RsWithMacrosInspectionVisitor() {
            @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
            override fun visitFunction2(func: RsFunction) {
                // Disable inside doc tests
                if (func.isDoctestInjection) return

                // Don't analyze functions with unresolved macro calls
                val hasUnresolvedMacroCall = func.descendantsWithMacrosOfType<RsMacroCall>().any { macroCall ->
                    val macro = macroCall.resolveToMacro()
                    macro == null || (!macro.hasRustcBuiltinMacro && macroCall.expansion == null)
                }
                if (hasUnresolvedMacroCall) return

                // Don't analyze functions with unresolved struct literals, e.g.:
                // let x = 1;
                // S { x }
                if (func.descendantsWithMacrosOfType<RsStructLiteral>().any { it.path.reference?.resolve() == null }) return

                // TODO: Remove this check when type inference is implemented for `asm!` macro calls
                if (func.descendantsWithMacrosOfType<RsAsmMacroArgument>().isNotEmpty()) return

                val liveness = func.liveness ?: return

                for (deadDeclaration in liveness.deadDeclarations) {
                    val name = deadDeclaration.binding.name ?: continue
                    if (name.startsWith("_")) continue
                    registerUnusedProblem(holder, deadDeclaration.binding, name, deadDeclaration.kind)
                }
            }
        }

    private fun registerUnusedProblem(
        holder: RsProblemsHolder,
        binding: RsPatBinding,
        name: String,
        kind: DeclarationKind
    ) {
        if (!binding.isPhysical) return

        if (binding.isCfgUnknown) return

        // TODO: remove this check when multi-resolve for `RsOrPat` is implemented
        if (binding.ancestorStrict<RsOrPat>() != null) return

        val isSimplePat = binding.topLevelPattern is RsPatIdent
        val message = if (isSimplePat) {
            when (kind) {
                Parameter -> "Parameter `$name` is never used"
                Variable -> "Variable `$name` is never used"
            }
        } else {
            "Binding `$name` is never used"
        }

        val fixes = mutableListOf<LocalQuickFix>(RenameFix(binding, "_$name"))
        if (isSimplePat) {
            when (kind) {
                Parameter -> fixes.add(RemoveParameterFix(binding, name))
                Variable -> {
                    if (binding.topLevelPattern.parent is RsLetDecl) {
                        fixes.add(RemoveVariableFix(binding, name))
                    }
                }
            }
        }

        holder.registerLintProblem(binding, message, RsLintHighlightingType.UNUSED_SYMBOL, fixes)
    }
}
