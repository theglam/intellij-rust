/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.wsl.sdk.flavor

import com.intellij.execution.wsl.WSLDistribution
import com.intellij.execution.wsl.WSLUtil
import com.intellij.util.io.isFile
import org.rust.ide.sdk.flavors.RsSdkFlavor
import org.rust.wsl.isWslP9SupportEnabled
import java.nio.file.Path

interface RsWslSdkFlavor : RsSdkFlavor {

    override fun isApplicable(): Boolean = WSLUtil.isSystemCompatible() && isWslP9SupportEnabled

    override fun isRemote(): Boolean = true

    override fun isValidSdkPath(sdkPath: Path): Boolean =
        sdkPath.toString().startsWith(WSLDistribution.UNC_PREFIX) && super.isValidSdkPath(sdkPath)

    override fun Path.hasExecutable(toolName: String): Boolean =
        resolve(toolName).toAbsolutePath().isFile()
}
