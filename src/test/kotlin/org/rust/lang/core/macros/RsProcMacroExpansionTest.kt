/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros

import org.rust.cargo.RsWithToolchainTestBase
import org.rust.cargo.project.model.cargoProjects
import org.rust.cargo.project.settings.rustSettings
import org.rust.cargo.runconfig.command.workingDirectory
import org.rust.cargo.toolchain.tools.rustup
import org.rust.ide.experiments.RsExperiments
import org.rust.lang.core.macros.tt.parseSubtree
import org.rust.lang.core.parser.createRustPsiBuilder
import org.rust.openapiext.runWithEnabledFeature
import org.rust.singleProject
import org.rust.singleWorkspace
import org.rust.stdext.RsResult

class RsProcMacroExpansionTest : RsWithToolchainTestBase() {
    fun `test 1`(): Unit = runWithEnabledFeature(RsExperiments.EVALUATE_BUILD_SCRIPTS) {
        buildProject {
            toml("Cargo.toml", """
                [workspace]
                members = ["my_proc_macro", "mylib"]
            """)
            dir("my_proc_macro") {
                toml("Cargo.toml", """
                    [package]
                    name = "my_proc_macro"
                    version = "1.0.0"
                    edition = "2018"

                    [lib]
                    proc-macro = true

                    [dependencies]
                """)
                dir("src") {
                    rust("lib.rs", """
                        extern crate proc_macro;
                        use proc_macro::TokenStream;

                        #[proc_macro]
                        pub fn my_macro(input: TokenStream) -> TokenStream {
                            return input;
                        }

                        #[proc_macro]
                        pub fn do_panic(input: TokenStream) -> TokenStream {
                            panic!("message");
                        }
                    """)
                }
            }
            dir("mylib") {
                toml("Cargo.toml", """
                    [package]
                    name = "mylib"
                    version = "1.0.0"

                    [dependencies]
                    my_proc_macro = { path = "../my_proc_macro" }
                """)
                dir("src") {
                    rust("lib.rs", """

                    """)
                }
            }
        }
        val pkg = project.cargoProjects.singleWorkspace().packages.find { it.name == "my_proc_macro" }!!
        val lib = pkg.procMacroArtifact!!.path.toString()
        val rustup = project.rustSettings.toolchain!!.rustup(project.cargoProjects.singleProject().workingDirectory)
        val server = ProcMacroServer.connect(rustup!!)
        val expander = ProcMacroExpander(project, server!!)

        expander.apply {
            checkError(lib, "do_panic", "")
            doTest(lib, "my_macro", "", "")
            doTest(lib, "my_macro", ".", ".")
            doTest(lib, "my_macro", "..", "..")
        }
    }

    private fun ProcMacroExpander.doTest(lib: String, name: String, macroCall: String, expected: String) {
        assertEquals(
            project.createRustPsiBuilder(expected).parseSubtree(),
            expand(project.createRustPsiBuilder(macroCall).parseSubtree(), name, lib).ok()
        )
    }

    private fun ProcMacroExpander.checkError(lib: String, name: String, macroCall: String) {
        val result = expand(project.createRustPsiBuilder(macroCall).parseSubtree(), name, lib)
        println(result)
        check(result is RsResult.Err)
    }
}
