/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.helpers.springcloud;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.intellij.helpers.UIHelperImpl;
import org.jetbrains.annotations.NotNull;

public class SpringCloudAppPropertyViewProvider implements FileEditorProvider, DumbAware {

    public static final String SPRING_CLOUD_APP_PROPERTY_TYPE = "SPRING_CLOUD_APP_PROPERTY";

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        return virtualFile.getFileType().getName().equals(getEditorTypeId());
    }

    @NotNull
    @Override
    public FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        final String clusterId = virtualFile.getUserData(UIHelperImpl.CLUSTER_ID);
        final String appId = virtualFile.getUserData(UIHelperImpl.APP_ID);
        return new SpringCloudAppPropertyView(project, clusterId, appId);
    }

    @NotNull
    @Override
    public String getEditorTypeId() {
        return SPRING_CLOUD_APP_PROPERTY_TYPE;
    }

    @NotNull
    @Override
    public FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }
}
