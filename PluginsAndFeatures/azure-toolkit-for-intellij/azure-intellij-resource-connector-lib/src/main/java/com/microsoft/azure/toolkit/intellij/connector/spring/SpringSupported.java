/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.spring;

import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.ResourceDefinition;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public interface SpringSupported<T> extends ResourceDefinition<T> {
    static List<Pair<String, String>> getProperties(Connection<?, ?> c) {
        final ResourceDefinition<?> rd = c.getResource().getDefinition();
        if (rd instanceof SpringSupported) {
            return ((SpringSupported<?>) rd).getSpringProperties().stream()
                    .map(p -> Pair.of(p.getKey(), p.getValue().replaceAll(Connection.ENV_PREFIX, c.getEnvPrefix())))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    List<Pair<String, String>> getSpringProperties();
}
