/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.component;

import com.microsoft.azure.toolkit.lib.common.entity.IAzureBaseResource;
import com.microsoft.azure.toolkit.lib.common.event.AzureEvent;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.event.AzureOperationEvent;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class AzureResourceLabelView<T extends IAzureBaseResource<?, ?>> implements NodeView {
    @Nonnull
    @Getter
    private final T resource;
    @Getter
    private final String label;
    private final AzureEventBus.EventListener<Object, AzureEvent<Object>> listener;
    @Getter
    private String description;
    @Nullable
    @Setter
    @Getter
    private Refresher refresher;

    public AzureResourceLabelView(@Nonnull T resource) {
        this.resource = resource;
        this.label = resource.name();
        this.listener = new AzureEventBus.EventListener<>(this::onEvent);
        AzureEventBus.on("resource.refresh.resource", listener);
        AzureEventBus.on("common|resource.status_changed", listener);
        this.refreshView();
    }

    public void onEvent(AzureEvent<Object> event) {
        final String type = event.getType();
        final Object source = event.getSource();
        if (source instanceof IAzureBaseResource && ((IAzureBaseResource<?, ?>) source).id().equals(this.resource.id())) {
            final AzureTaskManager tm = AzureTaskManager.getInstance();
            if ("resource.refresh.resource".equals(type)) {
                if (((AzureOperationEvent) event).getStage() == AzureOperationEvent.Stage.AFTER) {
                    tm.runLater(this::refreshChildren);
                }
            } else if ("common|resource.status_changed".equals(type)) {
                tm.runLater(this::refreshView);
            }
        }
    }

    public void dispose() {
        AzureEventBus.off("resource.refresh.resource", listener);
        AzureEventBus.off("common|resource.status_changed", listener);
        this.refresher = null;
    }

    public String getIconPath() {
        final String status = resource.status();
        final String type = resource.getClass().getSimpleName().toLowerCase();
        final String icon = StringUtils.isBlank(status) ? type : String.format("%s-%s", type, status.toLowerCase().trim());
        return String.format("/icons/%s.svg", icon);
    }
}
