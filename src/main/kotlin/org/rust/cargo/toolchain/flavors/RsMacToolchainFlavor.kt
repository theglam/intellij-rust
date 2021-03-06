/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain.flavors

import com.intellij.util.io.isDirectory
import org.rust.stdext.toPath
import java.nio.file.Path

object RsMacToolchainFlavor : RsToolchainFlavor() {
    override fun getHomePathCandidates(): List<Path> {
        val path = "/usr/local/Cellar/rust/bin".toPath()
        return if (path.isDirectory()) {
            listOf(path)
        } else {
            emptyList()
        }
    }
}
