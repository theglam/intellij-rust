/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.wsl

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.wsl.WSLDistribution
import com.intellij.execution.wsl.WSLDistributionWithRoot
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.io.exists
import com.intellij.util.io.isFile
import org.rust.cargo.toolchain.RsToolchain
import org.rust.cargo.toolchain.RsToolchainProvider
import org.rust.stdext.toPath
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

object RsWslToolchainProvider : RsToolchainProvider {
    override fun isApplicable(homePath: String): Boolean =
        homePath.startsWith(WSLDistribution.UNC_PREFIX)

    override fun getToolchain(homePath: String, toolchainName: String?): RsToolchain? {
        val (wslPath, distribution) = parseUncPath(homePath) ?: return null
        return RsWslToolchain(wslPath.toPath(), toolchainName, distribution)
    }
}

class RsWslToolchain(
    location: Path,
    name: String?,
    distribution: WSLDistribution
) : RsToolchain(location, name) {
    private val distribution = WSLDistributionWithRoot(distribution)

    override val fileSeparator: String = "/"

    override fun <T : GeneralCommandLine> patchCommandLine(commandLine: T): T {
        val parameters = commandLine.parametersList.list.map { toRemotePath(it) }
        commandLine.parametersList.clearAll()
        commandLine.parametersList.addAll(parameters)

        commandLine.environment.forEach { (k, v) ->
            val paths = v.split(File.pathSeparatorChar)
            commandLine.environment[k] = paths.joinToString(":") { toRemotePath(it) }
        }

        commandLine.workDirectory?.let {
            if (it.path.startsWith(fileSeparator)) {
                commandLine.workDirectory = File(toLocalPath(it.path))
            }
        }

        val remoteWorkDir = commandLine.workDirectory?.toString()?.let { toRemotePath(it) }
        val remoteCommandLine = distribution.patchCommandLine(commandLine, null, remoteWorkDir, false)
        // TODO: use more general solution
        val lastParameter = "source ~/.profile && " + remoteCommandLine.parametersList.last
        val parametersCount = remoteCommandLine.parametersList.parameters.size
        remoteCommandLine.parametersList[parametersCount - 1] = lastParameter
        return remoteCommandLine
    }

    override fun startProcess(commandLine: GeneralCommandLine): ProcessHandler = RsWslProcessHandler(commandLine)

    override fun toLocalPath(remotePath: String): String =
        distribution.getWindowsPath(FileUtil.toSystemIndependentName(remotePath)) ?: remotePath

    override fun toRemotePath(localPath: String): String =
        distribution.getWslPath(localPath) ?: localPath

    override fun expandUserHome(remotePath: String): String =
        distribution.expandUserHome(remotePath)

    override fun getExecutableName(toolName: String): String = toolName

    override fun pathToExecutable(toolName: String): Path {
        val exeName = getExecutableName(toolName)
        return location.resolve(exeName)
    }

    override fun pathToCargoExecutable(toolName: String): Path {
        val exePath = pathToExecutable(toolName)
        if (exePath.exists()) return exePath
        val cargoBin = expandUserHome("~/.cargo/bin")
        val exeName = getExecutableName(toolName)
        return Paths.get(cargoBin, exeName)
    }

    override fun hasExecutable(exec: String): Boolean =
        distribution.toUncPath(pathToExecutable(exec).toString()).toPath().isFile()

    override fun hasCargoExecutable(exec: String): Boolean =
        distribution.toUncPath(pathToCargoExecutable(exec).toString()).toPath().isFile()
}
