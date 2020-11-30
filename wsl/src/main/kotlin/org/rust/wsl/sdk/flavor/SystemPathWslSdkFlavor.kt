/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.wsl.sdk.flavor

import com.intellij.execution.wsl.WSLUtil
import org.rust.stdext.toPath
import java.io.File
import java.nio.file.Path

object SystemPathWslSdkFlavor : RsWslSdkFlavor {
    @Suppress("UnstableApiUsage")
    override fun getHomePathCandidates(): List<Path> {
        val paths = mutableListOf<Path>()
        for (distro in WSLUtil.getAvailableDistributions()) {
            val sysPath = distro.environment["PATH"] ?: continue
            val uncRoot = distro.uncRoot
            for (root in sysPath.split(":")) {
                if (root.isEmpty()) continue
                val file = File(uncRoot, root)
                if (!file.isDirectory) continue
                paths.add(file.absolutePath.toPath())
            }
        }
        return paths
    }
}
