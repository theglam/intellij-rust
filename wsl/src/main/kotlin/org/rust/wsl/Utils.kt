/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.wsl

import com.intellij.execution.wsl.WSLDistribution
import com.intellij.execution.wsl.WSLUtil
import com.intellij.openapi.application.Experiments
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.util.io.FileUtil
import org.rust.ide.sdk.remote.RsRemoteSdkAdditionalData
import org.rust.wsl.sdk.RsWslCredentialsHolder
import org.rust.wsl.sdk.RsWslCredentialsType

val isWslP9SupportEnabled: Boolean
    get() = Experiments.getInstance().isFeatureEnabled("wsl.p9.support")

val Sdk.isWsl: Boolean
    get() = (sdkAdditionalData as? RsRemoteSdkAdditionalData)?.isWsl == true

val RsRemoteSdkAdditionalData.isWsl: Boolean
    get() = remoteConnectionType == RsWslCredentialsType

val Sdk.distribution: WSLDistribution?
    get() = (sdkAdditionalData as? RsRemoteSdkAdditionalData)?.distribution

val RsRemoteSdkAdditionalData.distribution: WSLDistribution?
    get() = wslCredentials?.distribution

val RsRemoteSdkAdditionalData.wslCredentials: RsWslCredentialsHolder?
    get() = connectionCredentials().credentials as? RsWslCredentialsHolder

fun WSLDistribution.expandUserHome(path: String): String {
    if (!path.startsWith("~/")) return path
    // TODO: Synchronous execution on EDT
    val userHome = environment["HOME"] ?: return path
    return "$userHome${path.substring(1)}"
}

fun WSLDistribution.toUncPath(wslPath: String): String =
    WSLDistribution.UNC_PREFIX + msId + FileUtil.toSystemDependentName(wslPath)

fun parseUncPath(uncPath: String): Pair<String, WSLDistribution>? {
    if (!uncPath.startsWith(WSLDistribution.UNC_PREFIX)) return null
    val path = FileUtil.toSystemIndependentName(uncPath.removePrefix(WSLDistribution.UNC_PREFIX))
    val index = path.indexOf('/')
    if (index == -1) return null
    val wslPath = path.substring(index)
    val distName = path.substring(0, index)
    val distribution = WSLUtil.getDistributionByMsId(distName) ?: return null
    return wslPath to distribution
}
