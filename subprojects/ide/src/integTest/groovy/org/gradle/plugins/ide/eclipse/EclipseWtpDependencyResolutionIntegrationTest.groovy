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


package org.gradle.plugins.ide.eclipse

class EclipseWtpDependencyResolutionIntegrationTest extends AbstractEclipseIntegrationSpec {

    def "conflict resolution work on component dependencies"() {
        setup:
        settingsFile <<
        """rootProject.name = 'root'
           include 'module-a', 'module-b'
        """
        buildFile <<
        """allprojects {
               apply plugin: 'eclipse-wtp'
               apply plugin: 'war'
               repositories { jcenter() }
           }
        """
        file('module-a/src/main/java').mkdirs()
        file('module-a/build.gradle') << "dependencies { compile 'com.google.guava:guava:18.0' } "
        file('module-b/src/main/java').mkdirs()
        file('module-b/build.gradle') <<
        """dependencies {
               compile 'com.google.guava:guava:17.0'
               compile project(':module-a')
           }
        """

        when:
        run "eclipse"

        then:
        wtpComponent('module-a').lib('guava-18.0.jar').assertDeployedAt('/WEB-INF/lib')
        wtpComponent('module-b').lib('guava-18.0.jar').assertDeployedAt('/WEB-INF/lib')
    }

}
