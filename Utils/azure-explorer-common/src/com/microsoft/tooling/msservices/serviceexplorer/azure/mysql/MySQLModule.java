/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.mysql;

import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.event.AzureOperationEvent;
import com.microsoft.azure.toolkit.lib.mysql.AzureMySql;
import com.microsoft.azure.toolkit.lib.mysql.MySqlServer;
import com.microsoft.azuretools.utils.AzureUIRefreshCore;
import com.microsoft.azuretools.utils.AzureUIRefreshListener;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.Node;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

public class MySQLModule extends AzureRefreshableNode implements MySQLModuleView {

    public static final String MODULE_NAME = "Azure Database for MySQL";
    private static final String MYSQL_DATABASE_MODULE_ID = MySQLModule.class.getName();

    public MySQLModule(final Node parent) {
        super(MYSQL_DATABASE_MODULE_ID, MODULE_NAME, parent);
        createListener();

        AzureEventBus.after("mysql.create_server.server", this::onMySqlServerCreatedOrRemoved);
        AzureEventBus.after("mysql.delete_server.server", this::onMySqlServerCreatedOrRemoved);
    }

    private void onMySqlServerCreatedOrRemoved(AzureOperationEvent.Source source) {
        this.load(true);
    }

    public void renderChildren(List<MySqlServer> servers) {
        for (final MySqlServer server : servers) {
            final MySQLNode node = new MySQLNode(this, server);
            addChildNode(node);
        }
    }

    @Override
    @Nonnull
    public AzureIconSymbol getIconSymbol() {
        return AzureIconSymbol.MySQL.MODULE;
    }

    @Override
    protected void refreshItems() {
        final List<MySqlServer> server = Azure.az(AzureMySql.class).list();
        this.removeAllChildNodes();
        this.renderChildren(server);
    }

    @Override
    public void removeNode(String sid, String id, Node node) {
        Optional.ofNullable(Azure.az(AzureMySql.class).subscription(sid).get(id)).ifPresent(MySqlServer::delete);
        removeDirectChildNode(node);
    }

    private void createListener() {
        AzureUIRefreshListener listener = new AzureUIRefreshListener() {
            @Override
            public void run() {
                if (event.opsType == null) {
                    return;
                }
                switch (event.opsType) {
                    case SIGNIN:
                    case SIGNOUT:
                        removeAllChildNodes();
                        break;
                    case REFRESH:
                        if (isMySQLModuleEvent(event.object)) {
                            load(true);
                        }
                        break;
                    default:
                        if (isMySQLModuleEvent(event.object) && hasChildNodes()) {
                            load(true);
                        }
                        break;
                }
            }
        };
        AzureUIRefreshCore.addListener("MYSQL_MODULE", listener);
    }

    private boolean isMySQLModuleEvent(Object eventObject) {
        return eventObject != null && eventObject instanceof MySqlServer;
    }

}
