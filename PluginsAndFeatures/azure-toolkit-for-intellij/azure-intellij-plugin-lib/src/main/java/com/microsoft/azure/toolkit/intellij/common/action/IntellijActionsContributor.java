/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.action;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.intellij.common.properties.IntellijShowPropertiesViewAction;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureBaseResource;

import java.util.Objects;

public class IntellijActionsContributor implements IActionsContributor {
    public static final int ORDER = ResourceCommonActionsContributor.ORDER + 1;

    @Override
    public void registerHandlers(AzureActionManager am) {
        am.<String>registerHandler(ResourceCommonActionsContributor.OPEN_URL, Objects::nonNull, BrowserUtil::browse);
        am.<IAzureBaseResource<?, ?>, AnActionEvent>registerHandler(ResourceCommonActionsContributor.SHOW_PROPERTIES,
                (s, e) -> Objects.nonNull(s) && Objects.nonNull(e.getProject()),
                (s, e) -> IntellijShowPropertiesViewAction.showPropertyView(s, Objects.requireNonNull(e.getProject())));
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
