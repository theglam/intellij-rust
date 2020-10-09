/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.rider.debugger

import com.intellij.openapi.project.Project
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriverConfiguration
import com.jetbrains.cidr.execution.debugger.backend.lldb.LLDBDriverConfiguration
import org.rust.debugger.RsDebuggerDriverConfigurationProvider

class RsRiderDebuggerDriverConfigurationProvider : RsDebuggerDriverConfigurationProvider {
    override fun getDebuggerDriverConfiguration(project: Project): DebuggerDriverConfiguration? {
        return LLDBDriverConfiguration()
    }
}
