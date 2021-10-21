/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.springcloud.component;

import com.microsoft.azure.toolkit.eclipse.common.component.AzureComboBox;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import org.eclipse.swt.widgets.Composite;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SpringCloudAppComboBox extends AzureComboBox<SpringCloudApp> {
    private SpringCloudCluster cluster;
    private final Map<String, SpringCloudApp> localItems = new HashMap<>();

    public SpringCloudAppComboBox(Composite parent) {
        super(parent);
    }

    @Override
    protected String getItemText(final Object item) {
        if (Objects.isNull(item)) {
            return EMPTY_ITEM;
        }
        final SpringCloudApp app = (SpringCloudApp) item;
        if (!app.exists()) {
            return "(New) " + app.name();
        }
        return app.name();
    }

    public void setCluster(SpringCloudCluster cluster) {
        if (Objects.equals(cluster, this.cluster)) {
            return;
        }
        this.cluster = cluster;
        if (cluster == null) {
            this.clear();
            return;
        }
        this.refreshItems();
    }

    @NotNull
    @Override
    @AzureOperation(
            name = "springcloud|app.list.cluster",
            params = {"this.cluster.name()"},
            type = AzureOperation.Type.SERVICE
    )
    protected List<? extends SpringCloudApp> loadItems() throws Exception {
        final List<SpringCloudApp> apps = new ArrayList<>();
        if (Objects.nonNull(this.cluster)) {
            if (!this.localItems.isEmpty()) {
                apps.add(this.localItems.get(this.cluster.name()));
            }
            apps.addAll(cluster.apps());
        }
        return apps;
    }

    public void addLocalItem(SpringCloudApp app) {
        final SpringCloudApp cached = this.localItems.get(app.getCluster().name());
        if (Objects.isNull(cached) || !Objects.equals(app.name(), cached.name())) {
            this.localItems.put(app.getCluster().name(), app);
            final List<SpringCloudApp> items = this.getItems();
            items.add(0, app);
            this.setItems(items);
        }
    }
}
