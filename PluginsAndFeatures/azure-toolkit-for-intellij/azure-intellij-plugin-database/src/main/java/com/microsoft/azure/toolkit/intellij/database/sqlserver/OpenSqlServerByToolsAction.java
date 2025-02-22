/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.sqlserver;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.database.IntellijDatasourceService;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.database.JdbcUrl;
import com.microsoft.azure.toolkit.lib.sqlserver.model.SqlServerEntity;
import com.microsoft.azuretools.ActionConstants;
import com.microsoft.intellij.util.AzureLoginHelper;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.sqlserver.SqlServerNode;

@Name(OpenSqlServerByToolsAction.ACTION_NAME)
public class OpenSqlServerByToolsAction extends NodeActionListener {

    public static final String ACTION_NAME = "Open by Database Tools";
    private static final String NAME_PREFIX = "SQL Server - %s";
    private static final String DEFAULT_DRIVER_CLASS_NAME = "com.microsoft.sqlserver.jdbc.SQLServerDriver";

    private final SqlServerNode node;
    private final Project project;

    public OpenSqlServerByToolsAction(SqlServerNode node) {
        super();
        this.node = node;
        this.project = (Project) node.getProject();
    }

    @Override
    public AzureIconSymbol getIconSymbol() {
        return AzureIconSymbol.SqlServer.CONNECT_TO_SERVER;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        AzureLoginHelper.requireSignedIn(project, () -> this.doActionPerformed(project));
    }

    @Override
    protected String getServiceName(NodeActionEvent event) {
        return ActionConstants.parse(ActionConstants.SqlServer.CONNECT_TO_SERVER).getServiceName();
    }

    @Override
    protected String getOperationName(NodeActionEvent event) {
        return ActionConstants.parse(ActionConstants.SqlServer.CONNECT_TO_SERVER).getOperationName();
    }

    @AzureOperation(name = "sqlserver.open_by_database_tools.server", params = {"this.node.getServer().entity().getName()"}, type = AzureOperation.Type.ACTION)
    private void doActionPerformed(Project project) {
        SqlServerEntity entity = node.getServer().entity();
        IntellijDatasourceService.DatasourceProperties properties = IntellijDatasourceService.DatasourceProperties.builder()
                .name(String.format(NAME_PREFIX, entity.getName()))
                .driverClassName(DEFAULT_DRIVER_CLASS_NAME)
                .url(JdbcUrl.sqlserver(entity.getFullyQualifiedDomainName()).toString())
                .username(entity.getAdministratorLoginName() + "@" + entity.getName())
                .build();
        IntellijDatasourceService.getInstance().openDataSourceManagerDialog(project, properties);
    }

}
