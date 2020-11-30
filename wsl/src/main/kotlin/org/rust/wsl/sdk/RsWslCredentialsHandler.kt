/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.wsl.sdk

import com.intellij.execution.wsl.WSLDistribution
import com.intellij.remote.ext.RemoteCredentialsHandlerBase
import org.jdom.Element

class RsWslCredentialsHandler(credentials: RsWslCredentialsHolder) : RemoteCredentialsHandlerBase<RsWslCredentialsHolder>(credentials) {

    override fun getId(): String = WSLDistribution.UNC_PREFIX + credentials.distributionId

    override fun save(rootElement: Element) {
        credentials.save(rootElement)
    }

    override fun getPresentableDetails(interpreterPath: String): String = "WSL: $interpreterPath"

    override fun load(rootElement: Element?) {
        if (rootElement != null) {
            credentials.load(rootElement)
        }
    }
}
