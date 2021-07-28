/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.tooling.msservices.components.DefaultLoader;

import java.util.concurrent.Callable;

public abstract class NodeActionListenerAsync extends NodeActionListener {
    private final String progressMessage;

    public NodeActionListenerAsync(@NotNull String progressMessage) {
        this.progressMessage = progressMessage;
    }

    /**
     * Async action.
     *
     * @param actionEvent event object.
     * @return ListenableFuture object.
     */
    public ListenableFuture<Void> actionPerformedAsync(final NodeActionEvent actionEvent) {
        Callable<Boolean> booleanCallable = beforeAsyncActionPerformed();

        boolean shouldRun = true;

        try {
            shouldRun = booleanCallable.call();
        } catch (Exception ignored) {
            // ignore
        }

        final SettableFuture<Void> future = SettableFuture.create();

        if (shouldRun) {
            DefaultLoader.getIdeHelper().runInBackground(actionEvent.getAction().getNode().getProject(),
                    progressMessage, true, false, null, () -> {
                        try {
                            actionPerformed(actionEvent);
                            future.set(null);
                        } catch (AzureCmdException e) {
                            future.setException(e);
                        }
                    });
        } else {
            future.set(null);
        }

        return future;
    }

    @NotNull
    protected abstract Callable<Boolean> beforeAsyncActionPerformed();
}
