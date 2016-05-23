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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.plugins.ide.eclipse.EclipsePlugin;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;
import org.gradle.plugins.ide.eclipse.model.EclipseWtpComponent;
import org.gradle.plugins.ide.eclipse.model.WbDependentModule;
import org.gradle.plugins.ide.eclipse.model.WbModuleEntry;
import org.gradle.plugins.ide.eclipse.model.WbResource;
import org.gradle.plugins.ide.eclipse.model.WtpComponent;
import org.gradle.plugins.ide.internal.IdeDependenciesExtractor;
import org.gradle.plugins.ide.internal.resolver.model.IdeProjectDependency;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class WtpComponentFactory {
    public void configure(final EclipseWtpComponent wtp, WtpComponent component) {
        List<WbModuleEntry> entries = Lists.newArrayList();
        entries.addAll(getEntriesFromSourceDirs(wtp));
        for (WbResource element : wtp.getResources()) {
            if (wtp.getProject().file(element.getSourcePath()).isDirectory()) {
                entries.add(element);
            }
        }
        entries.addAll(wtp.getProperties());
        // for ear files root deps are NOT transitive; wars don't use root deps so this doesn't hurt them
        // TODO: maybe do this in a more explicit way, via config or something
        Project project = wtp.getProject();
        entries.addAll(getEntriesFromProjectDependencies(project, configOrEmptySet(wtp.getRootConfigurations()), configOrEmptySet(wtp.getMinusConfigurations()), "/"));
        entries.addAll(getEntriesFromProjectDependencies(project, configOrEmptySet(wtp.getLibConfigurations()), configOrEmptySet(wtp.getMinusConfigurations()), wtp.getLibDeployPath()));
        component.configure(wtp.getDeployName(), wtp.getContextPath(), entries);
    }

    private Set<Configuration> configOrEmptySet(Set<Configuration> configuration) {
        if (configuration == null) {
            return Sets.newHashSet();
        } else {
            return configuration;
        }
    }

    private List<WbResource> getEntriesFromSourceDirs(EclipseWtpComponent wtp) {
        List<WbResource> result = Lists.newArrayList();
        if (wtp.getSourceDirs() != null) {
            for (File dir : wtp.getSourceDirs()) {
                if (dir.isDirectory()) {
                    result.add(new WbResource(wtp.getClassesDeployPath(), wtp.getProject().relativePath(dir)));
                }
            }
        }
        return result;
    }

    // must include transitive project dependencies
    private List<WbDependentModule> getEntriesFromProjectDependencies(Project project, Set<Configuration> plusConfigurations, Set<Configuration> minusConfigurations, String deployPath) {
        IdeDependenciesExtractor extractor = new IdeDependenciesExtractor();
        Collection<IdeProjectDependency> dependencies = extractor.extractProjectDependencies(project, plusConfigurations, minusConfigurations);

        List<WbDependentModule> projectDependencies = Lists.newArrayList();
        for (IdeProjectDependency dependency : dependencies) {
            Project dependencyProject = dependency.getProject();
            String moduleName;
            if (dependencyProject.getPlugins().hasPlugin(EclipsePlugin.class)) {
                moduleName = dependencyProject.getExtensions().getByType(EclipseModel.class).getProject().getName();
            } else {
                moduleName = dependencyProject.getName();
            }
            projectDependencies.add(new WbDependentModule(deployPath, "module:/resource/" + moduleName + "/" + moduleName));
        }
        return projectDependencies;
    }
}
