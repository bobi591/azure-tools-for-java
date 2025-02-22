/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.storage;

import com.microsoft.azure.toolkit.ide.common.IExplorerContributor;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.ide.common.component.AzureResourceLabelView;
import com.microsoft.azure.toolkit.ide.common.component.AzureServiceLabelView;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureResource;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.storage.service.AzureStorageAccount;
import com.microsoft.azure.toolkit.lib.storage.service.StorageAccount;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class StorageExplorerContributor implements IExplorerContributor {
    private static final String NAME = "Storage Account";
    private static final String ICON = "/icons/storageaccount.svg";

    @Override
    public Node<?> getModuleNode() {
        final AzureActionManager am = AzureActionManager.getInstance();
        final IAzureMessager messager = AzureMessager.getDefaultMessager();

        final AzureStorageAccount service = az(AzureStorageAccount.class);
        return new Node<>(service).view(new AzureServiceLabelView<>(service, NAME, ICON))
                .actions(StorageActionsContributor.SERVICE_ACTIONS)
                .addChildren(this::listStorageAccounts, (account, storageNode) -> new Node<>(account)
                        .view(new AzureResourceLabelView<>(account))
                        .actions(StorageActionsContributor.ACCOUNT_ACTIONS));
    }

    @Nonnull
    private List<StorageAccount> listStorageAccounts(AzureStorageAccount s) {
        return s.list().stream().sorted(Comparator.comparing(IAzureResource::name)).collect(Collectors.toList());
    }
}
