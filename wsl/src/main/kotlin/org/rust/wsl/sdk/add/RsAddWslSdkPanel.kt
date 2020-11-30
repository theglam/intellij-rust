/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.wsl.sdk.add

import com.intellij.execution.wsl.WSLDistribution
import com.intellij.execution.wsl.WSLUtil
import com.intellij.icons.AllIcons
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.Disposer
import com.intellij.ui.layout.panel
import org.rust.ide.sdk.RsDetectedSdk
import org.rust.ide.sdk.RsSdkAdditionalDataPanel
import org.rust.ide.sdk.RsSdkAdditionalDataPanel.Companion.validateSdkAdditionalDataPanel
import org.rust.ide.sdk.RsSdkPathChoosingComboBox
import org.rust.ide.sdk.RsSdkPathChoosingComboBox.Companion.addToolchainsAsync
import org.rust.ide.sdk.RsSdkPathChoosingComboBox.Companion.validateSdkComboBox
import org.rust.ide.sdk.RsSdkUtils.detectRustSdks
import org.rust.ide.sdk.add.RsAddSdkPanel
import org.rust.ide.sdk.remote.RsRemoteSdkAdditionalData
import org.rust.wsl.parseUncPath
import org.rust.wsl.sdk.RsWslCredentialsType
import org.rust.wsl.sdk.flavor.RsWslSdkFlavor
import java.awt.BorderLayout
import java.awt.event.ItemEvent
import javax.swing.Icon
import javax.swing.JTextPane

private const val URL: String = "ms-windows-store://search/?query=Linux"
private const val MESSAGE: String = "<html>You don't have WSL distribution installed. <a href=\"$URL\">Install WSL distributions.</a></html>"

class RsAddWslSdkPanel(private val existingSdks: List<Sdk>) : RsAddSdkPanel() {
    override val panelName: String = "WSL"
    override val icon: Icon = AllIcons.RunConfigurations.Wsl

    private val sdkPathComboBox: RsSdkPathChoosingComboBox = RsSdkPathChoosingComboBox { path ->
        if (path != null && !path.startsWith(WSLDistribution.UNC_PREFIX)) {
            throw Exception("The selected directory is not a valid home for WSL Rust toolchain")
        }
    }
    private val homePath: String? get() = sdkPathComboBox.selectedSdk?.homePath

    private val sdkAdditionalDataPanel: RsSdkAdditionalDataPanel = RsSdkAdditionalDataPanel()
    private val data: RsRemoteSdkAdditionalData?
        get() {
            val baseData = sdkAdditionalDataPanel.data ?: return null
            val (wslPath, wslDistribution) = homePath?.let { parseUncPath(it) } ?: return null
            val data = RsRemoteSdkAdditionalData(wslPath)
            val credentials = RsWslCredentialsType.createCredentials()
            credentials.distributionId = wslDistribution.id
            data.setCredentials(RsWslCredentialsType.credentialsKey, credentials)
            baseData.copyTo(data)
            // TODO: fix stdlib path
            return data
        }

    init {
        if (!WSLUtil.hasAvailableDistributions()) {
            layout = BorderLayout()
            add(Messages.configureMessagePaneUi(JTextPane(), MESSAGE), BorderLayout.NORTH)
        } else {
            initUI()
        }
    }

    private fun initUI() {
        layout = BorderLayout()
        val formPanel = panel {
            row("Toolchain path:") { sdkPathComboBox() }
            sdkAdditionalDataPanel.attachTo(this)
        }
        add(formPanel, BorderLayout.NORTH)

        sdkPathComboBox.childComponent.addItemListener { event ->
            if (event.stateChange == ItemEvent.SELECTED) {
                sdkAdditionalDataPanel.notifySdkHomeChanged(homePath)
            }
        }

        addToolchainsAsync(sdkPathComboBox) {
            detectRustSdks(existingSdks) { it is RsWslSdkFlavor }
        }
    }

    override fun getOrCreateSdk(): Sdk? =
        when (val sdk = sdkPathComboBox.selectedSdk) {
            is RsDetectedSdk -> data?.let { sdk.setup(existingSdks, it) }
            else -> sdk
        }

    override fun validateAll(): List<ValidationInfo> = listOfNotNull(
        validateWslDistributions(),
        validateSdkComboBox(sdkPathComboBox),
        validateSdkAdditionalDataPanel(sdkAdditionalDataPanel)
    )

    override fun dispose() {
        Disposer.dispose(sdkAdditionalDataPanel)
    }

    companion object {
        private fun validateWslDistributions(): ValidationInfo? =
            if (!WSLUtil.hasAvailableDistributions()) {
                ValidationInfo("Can't find installed WSL distribution. Make sure you have one.")
            } else {
                null
            }
    }
}
