/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.crates.local

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapiext.isUnitTestMode
import org.rust.ide.experiments.RsExperiments
import org.rust.openapiext.isFeatureEnabled

class CratesLocalIndexStartupActivity : StartupActivity {
    override fun runActivity(project: Project) {
        if (!isUnitTestMode && isFeatureEnabled(RsExperiments.CRATES_LOCAL_INDEX)) {
            CratesLocalIndexService.getInstance()
        }
    }
}
