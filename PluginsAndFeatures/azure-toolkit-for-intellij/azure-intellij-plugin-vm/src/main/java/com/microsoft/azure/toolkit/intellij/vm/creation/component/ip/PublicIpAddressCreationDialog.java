/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.vm.creation.component.ip;

import com.intellij.ui.components.JBLabel;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.intellij.common.SwingUtils;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessageBundle;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.ResourceGroup;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.compute.ip.DraftPublicIpAddress;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Collections;
import java.util.List;

public class PublicIpAddressCreationDialog extends AzureDialog<DraftPublicIpAddress>
        implements AzureForm<DraftPublicIpAddress> {
    private static final String IP_NAME_PATTERN = "[a-zA-Z0-9]([a-zA-Z0-9_\\.-]{0,78}[a-zA-Z0-9_])?";

    private Subscription subscription;
    private ResourceGroup resourceGroup;
    private Region region;
    private JBLabel labelDescription;
    private JPanel contentPanel;
    private AzureTextInput txtName;

    public PublicIpAddressCreationDialog(@Nonnull Subscription subscription, @Nonnull ResourceGroup resourceGroup, @Nonnull Region region) {
        super();
        this.init();
        this.subscription = subscription;
        this.resourceGroup = resourceGroup;
        this.region = region;
        SwingUtils.setTextAndEnableAutoWrap(this.labelDescription, AzureMessageBundle.message("vm.publicIpAddress.description").toString());
        this.pack();
    }

    @Override
    public AzureForm<DraftPublicIpAddress> getForm() {
        return this;
    }

    @Override
    protected String getDialogTitle() {
        return AzureMessageBundle.message("vm.publicIpAddress.create.title").toString();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return this.contentPanel;
    }

    @Override
    public DraftPublicIpAddress getValue() {
        final DraftPublicIpAddress draftPublicIpAddress = new DraftPublicIpAddress(this.subscription.getId(), this.resourceGroup.getName(), this.txtName.getValue());
        draftPublicIpAddress.setRegion(region);
        return draftPublicIpAddress;
    }

    @Override
    public void setValue(final DraftPublicIpAddress data) {
        this.subscription = data.subscription();
        this.txtName.setValue(data.getName());
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Collections.singletonList(this.txtName);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        txtName = new AzureTextInput();
        txtName.setRequired(true);
        txtName.addValidator(() -> validatePublicIpName());
    }

    private AzureValidationInfo validatePublicIpName() {
        final String ipName = txtName.getValue();
        if (StringUtils.isEmpty(ipName) || ipName.length() > 80) {
            return AzureValidationInfo.error("The name must be between 1 and 80 characters.", txtName);
        } else if (!ipName.matches(IP_NAME_PATTERN)) {
            return AzureValidationInfo.error("The name must begin with a letter or number, end with a letter, number or underscore, " +
                    "and may contain only letters, numbers, underscores, periods, or hyphens.", txtName);
        }
        return AzureValidationInfo.success(txtName);
    }
}
