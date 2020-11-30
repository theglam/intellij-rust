/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.wsl.sdk

import com.intellij.execution.wsl.WSLDistribution
import com.intellij.execution.wsl.WSLUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.remote.CredentialsType
import com.intellij.remote.ext.CredentialsEditor
import com.intellij.remote.ext.CredentialsLanguageContribution
import com.intellij.remote.ext.RemoteCredentialsHandler
import com.intellij.remote.ui.BundleAccessor
import com.intellij.remote.ui.CredentialsEditorProvider
import com.intellij.remote.ui.RemoteSdkEditorForm
import org.rust.wsl.isWslP9SupportEnabled

object RsWslCredentialsType : CredentialsType<RsWslCredentialsHolder>("WSL", WSLDistribution.UNC_PREFIX),
                              CredentialsEditorProvider {

    private val RS_WSL_CREDENTIALS_KEY: Key<RsWslCredentialsHolder> = Key.create("RS_WSL_CREDENTIALS_HOLDER")

    override fun getCredentialsKey(): Key<RsWslCredentialsHolder> = RS_WSL_CREDENTIALS_KEY

    override fun getHandler(credentials: RsWslCredentialsHolder): RemoteCredentialsHandler =
        RsWslCredentialsHandler(credentials)

    override fun createCredentials(): RsWslCredentialsHolder = RsWslCredentialsHolder()

    override fun isAvailable(languageContribution: CredentialsLanguageContribution<*>?): Boolean =
        languageContribution is RsWslCredentialsContribution
            && WSLUtil.hasAvailableDistributions()
            && isWslP9SupportEnabled

    override fun createEditor(
        project: Project?,
        languageContribution: CredentialsLanguageContribution<*>?,
        parentForm: RemoteSdkEditorForm
    ): CredentialsEditor<*> = RsWslCredentialsEditor()

    override fun getDefaultInterpreterPath(bundleAccessor: BundleAccessor): String = "/usr/bin"

    override fun getWeight(): Int = 50
}
