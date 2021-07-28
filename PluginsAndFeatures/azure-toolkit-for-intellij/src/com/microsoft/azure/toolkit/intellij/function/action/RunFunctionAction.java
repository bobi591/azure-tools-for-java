package com.microsoft.azure.toolkit.intellij.function.action;

import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.impl.RunDialog;
import com.intellij.ide.IdeView;
import com.intellij.ide.actions.CreateElementActionBase;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.microsoft.azure.toolkit.intellij.function.Constants;
import com.microsoft.azure.toolkit.intellij.function.runner.AzureFunctionSupportConfigurationType;
import com.microsoft.azure.toolkit.intellij.function.runner.core.CLIExecutor;
import com.microsoft.azure.toolkit.intellij.function.runner.localrun.FunctionRunConfigurationFactory;
import com.microsoft.intellij.actions.RunConfigurationUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class RunFunctionAction extends CreateElementActionBase {

    private final AzureFunctionSupportConfigurationType configType = AzureFunctionSupportConfigurationType.getInstance();
    private String funcName = null;

    @Override
    protected PsiElement @NotNull [] create(@NotNull String s, PsiDirectory psiDirectory) throws Exception {
        return new PsiElement[0];
    }

    @Override
    protected @NlsContexts.DialogTitle String getErrorTitle() {
        return null;
    }

    @Override
    protected @NlsContexts.Command String getActionName(PsiDirectory psiDirectory, String s) {
        return null;
    }

    @Override
    protected void invokeDialog(@NotNull Project project, @NotNull PsiDirectory directory, @NotNull Consumer<PsiElement[]> elementsConsumer) {

        final RunManagerEx manager = RunManagerEx.getInstanceEx(project);
        final ConfigurationFactory factory = new FunctionRunConfigurationFactory(configType);
        final RunnerAndConfigurationSettings settings = RunConfigurationUtils.getOrCreateRunConfigurationSettings(funcName, project, manager, factory);
        if (RunDialog.editConfiguration(project, settings, message("function.run.configuration.title"), DefaultRunExecutor.getRunExecutorInstance())) {
            manager.addConfiguration(settings);
            manager.setSelectedConfiguration(settings);
            ProgramRunnerUtil.executeConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance());
        }
    }

    @Override
    protected boolean isAvailable(AnActionEvent e) {
        DataContext dataContext = e.getDataContext();
        List<String> missingFiles = new ArrayList<>();
        final Project project = CommonDataKeys.PROJECT.getData(dataContext);
        if(project == null) {
            return false;
        }
        final IdeView view = LangDataKeys.IDE_VIEW.getData(dataContext);
        if(view != null) {
            for(PsiDirectory directory : view.getDirectories())
            {
                for(String fileName : Constants.UPLOAD_FUNC_FILES) {
                    if(directory.findFile(fileName) == null && !missingFiles.contains(fileName)) {
                        missingFiles.add(fileName);
                    }
                }
                if(missingFiles.size() == 0) {
                    if(!CLIExecutor.runningProcesses.containsKey(directory.getName())) {
                        funcName = directory.getName();
                        return true;
                    }
                    else {
                        e.getPresentation().setEnabled(false);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        boolean visible = this.isAvailable(e);
        e.getPresentation().setVisible(visible);
    }

}
