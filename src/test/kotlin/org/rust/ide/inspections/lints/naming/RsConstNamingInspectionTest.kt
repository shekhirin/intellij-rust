/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints.naming

import org.rust.ide.inspections.RsInspectionsTestBase
import org.rust.ide.inspections.lints.RsConstNamingInspection

class RsConstNamingInspectionTest : RsInspectionsTestBase(RsConstNamingInspection::class) {
    fun `test constants`() = checkByText("""
        const CONST_OK: u32 = 12;
        const <warning descr="Constant `const_foo` should have an upper case name such as `CONST_FOO`">const_foo</warning>: u32 = 12;
    """)

    fun `test constants suppression`() = checkByText("""
        #[allow(non_upper_case_globals)]
        const const_foo: u32 = 12;
    """)

    fun `test constants suppression nonstandard style`() = checkByText("""
        #[allow(nonstandard_style)]
        const const_foo: u32 = 12;
    """)

    fun `test constants fix`() = checkFixByText("Rename to `CONST_FOO`", """
        const <warning descr="Constant `ConstFoo` should have an upper case name such as `CONST_FOO`">Con<caret>stFoo</warning>: u32 = 42;
        fn const_use() {
            let a = ConstFoo;
        }
    """, """
        const CONST_FOO: u32 = 42;
        fn const_use() {
            let a = CONST_FOO;
        }
    """, preview = null)

    fun `test constant not support case`() = checkByText("""
        const 常量: u32 = 12;
    """)
}
