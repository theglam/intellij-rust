/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.wsl.sdk

import com.intellij.openapi.projectRoots.Sdk
import com.intellij.util.io.exists
import org.rust.ide.sdk.RsSdkValidator
import org.rust.ide.sdk.remote.RsRemoteSdkAdditionalData
import org.rust.stdext.toPath
import org.rust.wsl.distribution
import org.rust.wsl.isWsl

object RsWslSdkValidator : RsSdkValidator {
    override fun isInvalid(sdk: Sdk): Boolean {
        if (!sdk.isWsl) return false
        val additionalData = sdk.sdkAdditionalData as? RsRemoteSdkAdditionalData
        val distribution = additionalData?.distribution ?: return true
        val homePath = distribution.getWindowsPath(additionalData.interpreterPath)?.toPath()
        return homePath == null || !homePath.exists()
    }
}
