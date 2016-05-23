/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.plugins.ide.eclipse.model.internal;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.plugins.ide.eclipse.model.AbstractLibrary;
import org.gradle.plugins.ide.eclipse.model.ClasspathEntry;
import org.gradle.plugins.ide.eclipse.model.Library;
import org.gradle.plugins.ide.eclipse.model.ProjectDependency;
import org.gradle.plugins.ide.internal.IdeDependenciesExtractor;
import org.gradle.plugins.ide.internal.resolver.model.IdeExtendedRepoFileDependency;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class WtpClasspathAttributeSupport {

    private static final String ATTRIBUTE_WTP_DEPLOYED = "org.eclipse.jst.component.dependency";
    private static final String ATTRIBUTE_WTP_NONDEPLOYED = "org.eclipse.jst.component.nondependency";

    private final String libDirName;
    private final boolean isUtilityProject;
    private final Set<ModuleVersionIdentifier> rootConfigModuleVersions;
    private final Set<ModuleVersionIdentifier> libConfigModuleVersions;
    private final Set<File> rootConfigFiles;
    private final Set<File> libConfigFiles;


    public WtpClasspathAttributeSupport(IdeDependenciesExtractor depsExtractor, boolean isUtilityProject,
                                        String libDirName, Set<Configuration> rootConfigs,
                                        Set<Configuration> libConfigs, Set<Configuration> minusConfigs) {
        this.isUtilityProject = isUtilityProject;
        this.libDirName = libDirName;
        this.rootConfigModuleVersions = Sets.newHashSet();
        this.rootConfigFiles = Sets.newHashSet();
        this.libConfigModuleVersions = Sets.newHashSet();
        this.libConfigFiles = Sets.newHashSet();
        populateVersionsAndFiles(depsExtractor, rootConfigs, minusConfigs, rootConfigModuleVersions, rootConfigFiles);
        populateVersionsAndFiles(depsExtractor, libConfigs, minusConfigs, libConfigModuleVersions, libConfigFiles);
    }

    private void populateVersionsAndFiles(IdeDependenciesExtractor depsExtractor, Set<Configuration> configs, Set<Configuration> minusConfigs, Set<ModuleVersionIdentifier> resultVersions, Set<File> resultFiles) {
        Collection<IdeExtendedRepoFileDependency> dependencies = depsExtractor.resolvedExternalDependencies(configs, minusConfigs);
        for (IdeExtendedRepoFileDependency dependency : dependencies) {
            resultVersions.add(dependency.getId());
            resultFiles.add(dependency.getFile());
        }
    }


    public Map<String, Object> createDeploymentAttribute(ClasspathEntry entry) {
        if (entry instanceof Library) {
            return createDeploymentAttribute((AbstractLibrary) entry);
        } else if (entry instanceof ProjectDependency) {
            return createDeploymentAttribute((ProjectDependency)entry);
        } else {
            return Collections.emptyMap();
        }
    }

    private Map<String, Object> createDeploymentAttribute(AbstractLibrary entry) {
        ModuleVersionIdentifier moduleVersion = entry.getModuleVersion();
        if (moduleVersion != null) {
            return createDeploymentAttribute(moduleVersion);
        } else {
            return createDeploymentAttribute(entry.getLibrary().getFile());
        }
    }

    private Map<String, Object> createDeploymentAttribute(ModuleVersionIdentifier moduleVersion) {
        if (!isUtilityProject) {
            if (rootConfigModuleVersions.contains(moduleVersion)) {
                return singleEntryMap(ATTRIBUTE_WTP_DEPLOYED, "/");
            } else if (libConfigModuleVersions.contains(moduleVersion)) {
                return singleEntryMap(ATTRIBUTE_WTP_DEPLOYED, libDirName);
            }
        }
        return singleEntryMap(ATTRIBUTE_WTP_NONDEPLOYED, "");
    }

    private Map<String, Object> createDeploymentAttribute(File file) {
        if (!isUtilityProject) {
            if (rootConfigFiles.contains(file)) {
                return singleEntryMap(ATTRIBUTE_WTP_DEPLOYED, "/");
            } else if (libConfigFiles.contains(file)) {
                return singleEntryMap(ATTRIBUTE_WTP_DEPLOYED, libDirName);
            }
        }
        return singleEntryMap(ATTRIBUTE_WTP_NONDEPLOYED, "");
    }

    private Map<String, Object> createDeploymentAttribute(ProjectDependency entry) {
        return singleEntryMap(ATTRIBUTE_WTP_NONDEPLOYED, "");
    }

    private static Map<String, Object> singleEntryMap(String key, String value) {
        return ImmutableMap.<String, Object>of(key, value);
    }
}
