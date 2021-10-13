/*
 * Copyright (c) 2020 JetBrains s.r.o.
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.aad;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.graph.models.Application;
import com.microsoft.graph.models.ImplicitGrantSettings;
import com.microsoft.graph.models.WebApplication;
import com.microsoft.graph.requests.GraphServiceClient;
import okhttp3.Request;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Displays UI to create a new Azure AD application.
 * <p>
 * ComponentNotRegistered is suppressed, because IntelliJ isn't finding the reference in resources/META-INF.
 */
@SuppressWarnings("ComponentNotRegistered")
public class RegisterApplicationAction extends AnAction {
    public RegisterApplicationAction() {
        super(MessageBundle.message("action.AzureToolkit.AD.AzureRegisterApp.text"));
    }

    @Override
    @AzureOperation(name = "connector|aad.register_application", type = AzureOperation.Type.ACTION)
    public void actionPerformed(@Nonnull AnActionEvent e) {
        var project = e.getProject();
        if (project == null || project.isDisposed()) {
            return;
        }

        // Show dialog to enter the application data, then create in background after user confirmed
        var dialog = new RegisterApplicationInAzureAdDialog(project);
        if (dialog.showAndGet()) {
            var subscription = dialog.getSubscription();
            if (subscription == null) {
                return;
            }

            var graphClient = AzureUtils.createGraphClient(subscription);
            var task = new RegisterApplicationTask(project, dialog.getForm().getData(), graphClient);
            var title = MessageBundle.message("action.azure.aad.registerApp.registeringApplication");

            AzureTaskManager.getInstance().runInBackground(title, task);
        }
    }

    /**
     * Task, which creates a new Azure AD application based on the data entered into the form.
     */
    static class RegisterApplicationTask implements Runnable {
        private final Project project;
        @Nonnull
        private final ApplicationRegistrationModel model;
        @Nonnull
        private final GraphServiceClient<Request> graphClient;

        RegisterApplicationTask(@Nonnull Project project,
                                @Nonnull ApplicationRegistrationModel model,
                                @Nonnull GraphServiceClient<Request> graphClient) {
            this.project = project;
            this.model = model;
            this.graphClient = graphClient;
        }

        @Override
        @AzureOperation(name = "connector|aad.create_aad_application", type = AzureOperation.Type.TASK)
        public void run() {
            var validSuffix = new StringBuilder();
            for (char c : (model.getDisplayName() + UUID.randomUUID().toString().substring(0, 6)).toCharArray()) {
                if (Character.isLetterOrDigit(c)) {
                    validSuffix.append(c);
                }
            }

            var params = new Application();
            params.displayName = model.getDisplayName();
            params.identifierUris = Collections.singletonList("https://" + model.getDomain() + "/" + validSuffix);
            params.web = new WebApplication();
            params.web.redirectUris = List.copyOf(model.getCallbackUrls());
            if (model.isMultiTenant()) {
                params.signInAudience = "AzureADMultipleOrgs";
            } else {
                params.signInAudience = "AzureADMyOrg";
            }
            // Grant "ID Tokens" permissions for the web application
            params.web.implicitGrantSettings = new ImplicitGrantSettings();
            params.web.implicitGrantSettings.enableIdTokenIssuance = true;

            // read-only access if the client ID was defined by the user
            var clientID = model.getClientId();
            if (StringUtil.isNotEmpty(clientID)) {
                params.appId = model.getClientId();
                showApplicationTemplateDialog(params);
                return;
            }

            var application = graphClient.applications().buildRequest().post(params);
            assert application.id != null;

            var newCredentials = AzureUtils.createApplicationClientSecret(graphClient, application);
            if (application.passwordCredentials == null) {
                application.passwordCredentials = Collections.singletonList(newCredentials);
            } else {
                application.passwordCredentials.add(newCredentials);
            }

            // now display the new application in the "Application templates dialog"
            showApplicationTemplateDialog(application);
        }

        @AzureOperation(name = "connector|aad.show_aad_template", type = AzureOperation.Type.TASK)
        private void showApplicationTemplateDialog(Application application) {
            AzureTaskManager.getInstance().runLater(() -> {
                new AzureApplicationTemplateDialog(project, application).show();
            });
        }
    }
}
