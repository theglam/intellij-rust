/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.rider.debugger.runconfig.legacy

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import org.rust.cargo.runconfig.BuildResult
import org.rust.debugger.runconfig.RsDebugRunnerBase
import org.rust.debugger.runconfig.legacy.RsDebugRunnerLegacyBase

class RsRiderDebugRunnerLegacy : RsDebugRunnerLegacyBase() {

    override fun checkToolchainSupported(host: String): BuildResult.ToolchainError? {
        val isMSVCRustToolchain = "msvc" in host
        return if (!isMSVCRustToolchain) {
            BuildResult.ToolchainError.MSVCWithRustGNU
        } else {
            null
        }
    }

    override fun checkToolchainConfigured(project: Project): Boolean = SystemInfo.isWindows
}
