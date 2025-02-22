/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.hdinsight.spark.console

import com.intellij.execution.CantRunException
import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.jarRepository.JarRepositoryManager
import com.intellij.jarRepository.RemoteRepositoriesConfiguration
import com.intellij.openapi.module.Module
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.util.DispatchThreadProgressWindow
import com.intellij.openapi.projectRoots.JavaSdkType
import com.intellij.openapi.projectRoots.JdkUtil
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.roots.libraries.NewLibraryConfiguration
import com.intellij.openapi.roots.ui.configuration.libraryEditor.NewLibraryEditor
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.ProjectScope
import com.intellij.util.PathUtil.getJarPathForClass
import com.microsoft.azure.hdinsight.spark.mock.SparkLocalConsoleMockFsAgent
import com.microsoft.azure.hdinsight.spark.run.SparkBatchLocalRunState
import com.microsoft.azure.hdinsight.spark.run.configuration.LivySparkBatchJobRunConfiguration
import com.microsoft.intellij.ui.ErrorWindow
import com.microsoft.intellij.util.runInWriteAction
import org.jetbrains.plugins.scala.console.configuration.ScalaConsoleRunConfiguration
import org.jdom.Element
import java.nio.file.Paths
import javax.swing.Action

class SparkScalaLocalConsoleRunConfiguration(
        private val scalaConsoleRunConfDelegate: ScalaConsoleRunConfiguration,
        private val isMockFs: Boolean)
    : ModuleBasedConfiguration<RunConfigurationModule, Element>(
        scalaConsoleRunConfDelegate.name,
        scalaConsoleRunConfDelegate.configurationModule,
        scalaConsoleRunConfDelegate.factory!!)
{

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return scalaConsoleRunConfDelegate.configurationEditor
    }

    override fun getValidModules(): MutableCollection<Module> {
        return scalaConsoleRunConfDelegate.validModules
    }

    override fun readExternal(element: Element) {
        scalaConsoleRunConfDelegate.readExternal(element)
    }

    override fun writeExternal(element: Element) {
        scalaConsoleRunConfDelegate.writeExternal(element)
    }

    private val sparkCoreCoodRegex = """.*\b(org.apache.spark:spark-)(core)(_.+:.+)""".toRegex()
    private val replMain = "org.apache.spark.repl.Main"

    lateinit var batchRunConfiguration: LivySparkBatchJobRunConfiguration

    val mainClass: String = "org.apache.spark.deploy.SparkSubmit"

    private fun createScalaParams(): JavaParameters {
        val module = configurationModule.module ?: throw ExecutionException("Module is not specified")

        val rootManager = ModuleRootManager.getInstance(module)
        val sdk = rootManager.sdk
        if (sdk == null || sdk.sdkType !is JavaSdkType) {
            throw CantRunException.noJdkForModule(module)
        }

        return JavaParameters().apply {
            vmParametersList.addParametersString("-Djline.terminal=NONE")
            setUseDynamicClasspath(JdkUtil.useDynamicClasspath(project))
            workingDirectory = workingDirectory
            mainClass = mainClass
            configureByModule(module, JavaParameters.JDK_AND_CLASSES_AND_TESTS)
        }
    }

    fun createJavaParams(): JavaParameters {
        val localRunParams = SparkBatchLocalRunState(project, batchRunConfiguration.model.localRunConfigurableModel, null)
                .createParams(executor = null, hasJmockit = isMockFs, hasMainClass = false, hasClassPath = false)
        val params = createScalaParams()
        params.classPath.clear()
        val replLibraryCoord = findReplCoord() ?: throw ExecutionException("""
                The library org.apache.spark:spark-core is not in project dependencies.
                The project may not be a Spark Application project.
                Please create it from the wizard or add Spark related libraries into dependencies.
                ( Refer to https://www.jetbrains.com/help/idea/library.html#add-library-to-module-dependencies )
        """.trimIndent())

        // Check repl dependence and prompt the user to fix it
        checkReplDependenceAndTryToFix(replLibraryCoord)
        params.classPath.addAll(getDependencyClassPaths(replLibraryCoord))

        // Workaround for Spark 2.3 jline issue, refer to:
        // - https://github.com/Microsoft/azure-tools-for-java/issues/2285
        // - https://issues.apache.org/jira/browse/SPARK-13710
        val jlineLibraryCoord = "jline:jline:2.14.5"
        if (getLibraryByCoord(jlineLibraryCoord) == null) {
            promptAndFix(jlineLibraryCoord)
        }
        params.classPath.addAll(getDependencyClassPaths(jlineLibraryCoord))

        params.classPath.addAll(localRunParams.classPath.pathList)
        params.mainClass = mainClass

        params.vmParametersList.addAll(localRunParams.vmParametersList.parameters)

        if (isMockFs) {
            params.vmParametersList.add("-javaagent:${getJarPathForClass(SparkLocalConsoleMockFsAgent::class.java)}")
        }

        params.workingDirectory = Paths.get(batchRunConfiguration.model.localRunConfigurableModel.dataRootDirectory, "__default__", "user", "current").toString()
        params.programParametersList.add("--class", "org.apache.spark.repl.Main")
        params.programParametersList.add("--name", "Spark shell")
        params.programParametersList.add("spark-shell")

        params.addEnv("SPARK_SUBMIT_OPTS", "-Dscala.usejavacp=true")
        localRunParams.env.forEach { name, value -> params.addEnv(name, value) }

        return params
    }

    private fun getDependencyClassPaths(libraryCoord: String): List<String> {
        val library = getLibraryByCoord(libraryCoord) ?: throw ExecutionException("""
                The library $libraryCoord is not in project dependencies, please add it as the top one of list.
                ( Refer to https://www.jetbrains.com/help/idea/library.html#add-library-to-module-dependencies )
                """.trimIndent())

        return library.getFiles(OrderRootType.CLASSES).map { it.presentableUrl }
    }

    private fun findReplCoord(): String? {
        val iterator = LibraryTablesRegistrar.getInstance().getLibraryTable(project).libraryIterator

        while (iterator.hasNext()) {
            val libEntryName = iterator.next().name ?: continue

            // Replace `core` to `repl` with the title removed, such as:
            //     Maven: org.apache.spark:spark-core_2.11:2.1.0 => org.apache.spark:spark-repl_2.11:2.1.0
            //     ^^^^^^^                       ^^^^                                      ^^^^
            val replCoord = sparkCoreCoodRegex.replace(libEntryName) { "${it.groupValues[1]}repl${it.groupValues[3]}" }

            if (replCoord != libEntryName) {
                // Found and replaced
                return replCoord
            }
        }

        return null
    }

    private fun checkReplDependenceAndTryToFix(replLibraryCoord: String) {
        if (getLibraryByCoord(replLibraryCoord) == null
                && JavaPsiFacade.getInstance(project).findClass(replMain, ProjectScope.getLibrariesScope(project)) == null) {
            // `repl.Main` is not in the project class path
            promptAndFix(replLibraryCoord)
        }
    }

    private fun promptAndFix(libraryCoord: String) {
        val toFixDialog = object : ErrorWindow(
                project,
                "The library $libraryCoord is not in project dependencies, would you like to auto fix it?",
                "Auto fix dependency issue to confirm") {
            init {
                setOKButtonText("Auto Fix")
            }

            override fun createActions(): Array<Action> {
                return arrayOf(okAction, cancelAction)
            }
        }

        val toFix = toFixDialog.showAndGet()

        if (toFix) {
            val progress = DispatchThreadProgressWindow(false, project).apply {
                setRunnable {
                    ProgressManager.getInstance().runProcess({
                        text = "Download $libraryCoord ..."
                        fixDependence(libraryCoord)
                    }, this@apply)
                }

                title = "Auto fix dependency $libraryCoord"
            }

            progress.start()
        }
    }

    private fun fixDependence(libraryCoord: String) {
        runInWriteAction {
            val projectRepositories = RemoteRepositoriesConfiguration.getInstance(project).repositories
            val newLibConf: NewLibraryConfiguration = JarRepositoryManager.resolveAndDownload(
                    project, libraryCoord, false, false, true, null, projectRepositories) ?: return@runInWriteAction
            val libraryType = newLibConf.libraryType
            val library = LibraryTablesRegistrar.getInstance().getLibraryTable(project).createLibrary("Apache Spark Console(auto-fix): $libraryCoord")

            val editor = NewLibraryEditor(libraryType, null)
            newLibConf.addRoots(editor)
            val model = library.modifiableModel
            editor.applyTo(model as LibraryEx.ModifiableModelEx)
            model.commit()
        }
    }

    private fun getLibraryByCoord(libraryCoord: String): Library? = LibraryTablesRegistrar.getInstance().getLibraryTable(project)
            .libraries.firstOrNull { it.name?.endsWith(libraryCoord) == true }

    override fun getState(executor: Executor, env: ExecutionEnvironment): RunProfileState? {
        val state = object : JavaCommandLineState(env) {
            override fun createJavaParameters() : JavaParameters {
                val params = createJavaParams()

                params.programParametersList.addParametersString(scalaConsoleRunConfDelegate.consoleArgs())
                return params
            }
        }

        batchRunConfiguration.configurationModule.setModuleToAnyFirstIfNotSpecified()

        state.consoleBuilder = SparkScalaConsoleBuilder(project, batchRunConfiguration.modules.firstOrNull()
                ?: throw ExecutionException(RuntimeConfigurationError(
                        "The default module needs to be set in the local run tab of Run Configuration")))

        return state
    }
}
