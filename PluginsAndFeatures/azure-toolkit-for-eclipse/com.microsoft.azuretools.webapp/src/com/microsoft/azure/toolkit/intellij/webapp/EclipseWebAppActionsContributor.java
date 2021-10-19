/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.webapp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerService;

import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.lib.appservice.model.AppServiceFile;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebApp;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureBaseResource;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.azureexplorer.editors.webapp.DeploymentSlotPropertyEditorInput;
import com.microsoft.azuretools.azureexplorer.editors.webapp.WebAppPropertyEditor;
import com.microsoft.azuretools.azureexplorer.editors.webapp.WebAppPropertyEditorInput;
import com.microsoft.azuretools.azureexplorer.helpers.EditorType;
import com.microsoft.tooling.msservices.components.DefaultLoader;

public class EclipseWebAppActionsContributor implements IActionsContributor {
	private static final String UNABLE_TO_OPEN_EXPLORER = "Unable to open explorer";

	@Override
	public void registerHandlers(AzureActionManager am) {
		final BiPredicate<IAzureBaseResource<?, ?>, Object> isWebApp = (r, e) -> r instanceof IWebApp;
		final BiConsumer<IAzureBaseResource<?, ?>, Object> openWebAppPropertyViewHandler = (c, e) -> AzureTaskManager
				.getInstance().runLater(() -> {
					IWorkbench workbench = PlatformUI.getWorkbench();
					WebAppPropertyEditorInput input = new WebAppPropertyEditorInput(((IWebApp) c).subscriptionId(),
							((IWebApp) c).id(), ((IWebApp) c).name());
					IEditorDescriptor descriptor = workbench.getEditorRegistry().findEditor(WebAppPropertyEditor.ID);
					openEditor(EditorType.WEBAPP_EXPLORER, input, descriptor);
				});
		am.registerHandler(ResourceCommonActionsContributor.SHOW_PROPERTIES, isWebApp, openWebAppPropertyViewHandler);

		final BiPredicate<IAzureBaseResource<?, ?>, Object> isWebAppSlot = (r, e) -> r instanceof IWebAppDeploymentSlot;
		final BiConsumer<IAzureBaseResource<?, ?>, Object> openWebAppSlotPropertyViewHandler = (c,
				e) -> AzureTaskManager.getInstance().runLater(() -> {
					IWorkbench workbench = PlatformUI.getWorkbench();
					DeploymentSlotPropertyEditorInput input = new DeploymentSlotPropertyEditorInput(
							((IWebAppDeploymentSlot) c).id(), ((IWebAppDeploymentSlot) c).subscriptionId(),
							((IWebAppDeploymentSlot) c).webApp().id(), ((IWebAppDeploymentSlot) c).name());
					IEditorDescriptor descriptor = workbench.getEditorRegistry().findEditor(WebAppPropertyEditor.ID);
					openEditor(EditorType.WEBAPP_EXPLORER, input, descriptor);
				});
		am.registerHandler(ResourceCommonActionsContributor.SHOW_PROPERTIES, isWebAppSlot,
				openWebAppSlotPropertyViewHandler);

		final BiConsumer<IAzureBaseResource<?, ?>, Object> deployWebAppHandler = (c, e) -> AzureTaskManager
				.getInstance().runLater(() -> {
					try {
						((IHandlerService) PlatformUI.getWorkbench().getService(IHandlerService.class)).executeCommand("com.microsoft.azuretools.webapp.commands.deployToAzure", null);
					} catch (ExecutionException | NotDefinedException | NotEnabledException | NotHandledException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				});
		am.registerHandler(ResourceCommonActionsContributor.DEPLOY, isWebApp, deployWebAppHandler);
		
		final BiPredicate<AppServiceFile, Object> appServiceFile = (r, e) -> r instanceof AppServiceFile;
		final BiConsumer<AppServiceFile, Object> fileDownloadHandler = (file, e) -> AzureTaskManager
				.getInstance().runLater(()->{
			        final File destFile = DefaultLoader.getUIHelper().showFileChooser(String.format("Download %s", file.getName()));
			        if (destFile == null) {
			            return;
			        }
			        final OutputStream output = new FileOutputStream(destFile);
			        final AzureString title = AzureOperationBundle.title("appservice|file.download", file.getName());
			        final AzureTask<Void> task = new AzureTask<>(null, title, false, () -> {
			            file.getApp()
			                    .getFileContent(file.getPath())
			                    .doOnComplete(() -> notifyDownloadSuccess(file.getName(), destFile, ((Project) context)))
			                    .doOnTerminate(() -> IOUtils.closeQuietly(output, null))
			                    .subscribe(bytes -> {
			                        try {
			                            if (bytes != null) {
			                                output.write(bytes.array(), 0, bytes.limit());
			                            }
			                        } catch (final IOException e) {
			                            final String error = "failed to write data into local file";
			                            final String action = "try later";
			                            throw new AzureToolkitRuntimeException(error, e, action);
			                        }
			                    }, IDEHelperImpl::onRxException);
			        });
			        AzureTaskManager.getInstance().runInModal(task);
				});
	}

	private void openEditor(EditorType type, IEditorInput input, IEditorDescriptor descriptor) {
		try {
			IWorkbench workbench = PlatformUI.getWorkbench();
			IWorkbenchWindow activeWorkbenchWindow = workbench.getActiveWorkbenchWindow();
			if (activeWorkbenchWindow == null) {
				return;
			}
			IWorkbenchPage page = activeWorkbenchWindow.getActivePage();
			if (page == null) {
				return;
			}
			page.openEditor(input, descriptor.getId());
		} catch (Exception e) {
			AzureMessager.getMessager().error(UNABLE_TO_OPEN_EXPLORER);
		}
	}

	public int getOrder() {
		return 2;
	}
}
