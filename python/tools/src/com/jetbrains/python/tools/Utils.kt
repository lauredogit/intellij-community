/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.python.tools

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.impl.ApplicationInfoImpl
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.util.ui.UIUtil
import com.jetbrains.python.PythonModuleTypeBase
import com.jetbrains.python.sdk.PythonSdkType
import com.jetbrains.python.sdk.PythonSdkUpdater
import com.jetbrains.python.tools.sdkTools.PySdkTools
import com.jetbrains.python.tools.sdkTools.SdkCreationType
import java.io.File

internal val TestPath = System.getenv("PYCHARM_PERF_ENVS")

@JvmOverloads
fun createSdkForPerformance(module: Module,
                            sdkCreationType: SdkCreationType,
                            sdkHome: String = File(TestPath, "envs/py36_64").absolutePath): Sdk {
  ApplicationInfoImpl.setInStressTest(true) // To disable slow debugging
  val executable = File(PythonSdkType.getPythonExecutable(sdkHome) ?: throw AssertionError("No python on $sdkHome"))
  println("Running on $sdkHome")
  return PySdkTools.createTempSdk(VfsUtil.findFileByIoFile(executable, true)!!, sdkCreationType, module)
}


fun openProjectWithSdk(projectPath: String, sdkHome: String?): Pair<Project?, Sdk?> {
  val project: Project? = ProjectManager.getInstance().loadAndOpenProject(projectPath)


  val module = getOrCreateModule(project!!, projectPath)

  val sdk =
    if (sdkHome != null) {
      val sdk = createSdkForPerformance(module, SdkCreationType.SDK_PACKAGES_AND_SKELETONS, sdkHome)

      UIUtil.invokeAndWaitIfNeeded(Runnable {
        ApplicationManager.getApplication().runWriteAction({
                                                             PythonSdkUpdater.update(sdk, null, project, null)
                                                           })
      })

      ModuleRootModificationUtil.setModuleSdk(module, sdk)

      assert(ModuleRootManager.getInstance(module).orderEntries().classesRoots.size > 5)

      sdk
    }
    else {
      null
    }


  assert(ModuleManager.getInstance(project).modules.size == 1)

  return Pair(project, sdk)
}

fun getOrCreateModule(project: Project, projectPath: String): Module {
  if (ModuleManager.getInstance(project).modules.isNotEmpty()) {
    return ModuleManager.getInstance(project).modules[0]
  }
  else {
    val module: Module = ApplicationManager.getApplication().runWriteAction(
      Computable<Module> { ModuleManager.getInstance(project).newModule(projectPath, PythonModuleTypeBase.PYTHON_MODULE) }
    )

    val root = VfsUtil.findFileByIoFile(File(projectPath), true)!!

    ModuleRootModificationUtil.updateModel(module, { t ->
      val e = t.addContentEntry(root)
      e.addSourceFolder(root, false)
    })

    return module
  }
}