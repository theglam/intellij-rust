/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.wsl.sdk.flavor

import com.intellij.execution.wsl.WSLUtil
import org.rust.cargo.toolchain.tools.Rustup
import org.rust.stdext.toPath
import org.rust.wsl.expandUserHome
import java.io.File
import java.nio.file.Path

object RustupWslSdkFlavor : RsWslSdkFlavor {

    override fun getHomePathCandidates(): List<Path> {
        val paths = mutableListOf<Path>()
        for (distro in WSLUtil.getAvailableDistributions()) {
            val cargoBin = distro.expandUserHome("~/.cargo/bin")
            val file = File(distro.uncRoot, cargoBin)
            if (!file.isDirectory) continue
            paths.add(file.absolutePath.toPath())
        }
        return paths
    }

    override fun isValidSdkPath(sdkPath: Path): Boolean =
        super.isValidSdkPath(sdkPath) && sdkPath.hasExecutable(Rustup.NAME)
}
