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

import com.google.common.collect.Sets;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.plugins.WarPlugin;
import org.gradle.plugins.ear.Ear;
import org.gradle.plugins.ear.EarPlugin;
import org.gradle.plugins.ide.eclipse.EclipseWtpPlugin;
import org.gradle.plugins.ide.eclipse.model.AbstractClasspathEntry;
import org.gradle.plugins.ide.eclipse.model.EclipseClasspath;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;
import org.gradle.plugins.ide.eclipse.model.EclipseWtp;
import org.gradle.plugins.ide.eclipse.model.EclipseWtpComponent;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WtpAwareDependenciesCreator extends DependenciesCreator {

    private static final String DEFAULT_DEPLOY_DIR_NAME = "/WEB-INF/lib";

    private final boolean utilityProject;
    private final String libDirName;
    private final Set<Configuration> wtpRootConfigs;
    private final Set<Configuration> wtpLibConfigs;
    private final Set<Configuration> wtpMinusConfigs;

    public WtpAwareDependenciesCreator(EclipseClasspath classpath) {
        super(classpath);
        Project project = classpath.getProject();

        utilityProject = !project.getPlugins().hasPlugin(WarPlugin.class) && !project.getPlugins().hasPlugin(EarPlugin.class);
        Ear ear = (Ear) project.getTasks().findByName(EarPlugin.EAR_TASK_NAME);
        libDirName = ear == null ? DEFAULT_DEPLOY_DIR_NAME : ear.getLibDirName();
        EclipseWtp eclipseWtp = project.getExtensions().findByType(EclipseModel.class).getWtp();

        EclipseWtpComponent wtpComponent = eclipseWtp.getComponent();
        wtpRootConfigs = wtpComponent.getRootConfigurations();
        wtpLibConfigs = wtpComponent.getLibConfigurations();
        wtpMinusConfigs = wtpComponent.getMinusConfigurations();
    }

    @Override
    protected Collection<Configuration> getPlusConfig() {
        Set<Configuration> plusConfigs = Sets.newLinkedHashSet();
        plusConfigs.addAll(super.getPlusConfig());
        plusConfigs.addAll(wtpRootConfigs);
        plusConfigs.addAll(wtpLibConfigs);
        return plusConfigs;
    }

    @Override
    protected Collection<Configuration> getMinusConfigs() {
        Set<Configuration> minusConfigs = Sets.newLinkedHashSet();
        minusConfigs.addAll(super.getMinusConfigs());
        minusConfigs.addAll(wtpMinusConfigs);
        return minusConfigs;
    }

    @Override
    public List<AbstractClasspathEntry> createDependencyEntries() {
        List<AbstractClasspathEntry> dependencyEntries = super.createDependencyEntries();
        WtpClasspathAttributeSupport wtpSupport = new WtpClasspathAttributeSupport(dependenciesExtractor, utilityProject, libDirName, wtpRootConfigs, wtpLibConfigs, wtpMinusConfigs);
        for (AbstractClasspathEntry entry : dependencyEntries) {
            Map<String, Object> attribute = wtpSupport.createDeploymentAttribute(entry);
            entry.getEntryAttributes().putAll(attribute);
        }
        return dependencyEntries;
    }

}
