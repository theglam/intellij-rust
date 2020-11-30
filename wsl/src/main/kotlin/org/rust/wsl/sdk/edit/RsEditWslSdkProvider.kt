/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.wsl.sdk.edit


import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.SdkModificator
import org.rust.ide.sdk.edit.RsEditSdkDialog
import org.rust.ide.sdk.edit.RsEditSdkProvider
import org.rust.ide.sdk.remote.RsRemoteSdkAdditionalData
import org.rust.wsl.isWsl

object RsEditWslSdkProvider : RsEditSdkProvider {

    override fun isApplicable(modificator: SdkModificator): Boolean {
        val additionalData = modificator.sdkAdditionalData as? RsRemoteSdkAdditionalData
        return additionalData?.isWsl == true
    }

    override fun createDialog(
        project: Project,
        modificator: SdkModificator,
        nameValidator: (String) -> String?
    ): RsEditSdkDialog = RsEditWslSdkDialog(
        project,
        modificator,
        nameValidator
    )
}
