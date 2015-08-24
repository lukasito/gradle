/*
 * Copyright 2015 the original author or authors.
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

package org.gradle.api.internal.tasks

import org.gradle.api.Action
import org.gradle.api.internal.AbstractTask
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.tasks.TaskContainer
import org.gradle.model.internal.core.MutableModelNode
import org.gradle.model.internal.fixture.ModelRegistryHelper
import spock.lang.Specification

class RealizableTaskCollectionTest extends Specification {

    def "realizes a nodes link of a given type"() {
        given:
        def project = Mock(ProjectInternal)
        ModelRegistryHelper registry = new ModelRegistryHelper()
        project.getModelRegistry() >> registry

        def events = []

        Action mutatorAction = mutator(registry, events, Mock(BasicTask), "tasks.basic")

        registry.createInstance("tasks", Mock(TaskContainer))
        registry.mutate { it.path "tasks" node mutatorAction }

        when:
        new RealizableTaskCollection(BasicTask, Mock(DefaultTaskCollection), project).realizeRuleTaskTypes()

        then:
        events == ['created task tasks.basic']
    }

    def "does not realise a node link for non-realisable types"() {
        given:
        def project = Mock(ProjectInternal)
        ModelRegistryHelper registry = new ModelRegistryHelper()
        project.getModelRegistry() >> registry

        def events = []

        Action basicAction = mutator(registry, events, Mock(BasicTask), "tasks.basic")
        Action redundantAction = mutator(registry, events, Mock(RedundantTask), "tasks.redundant")

        registry.createInstance("tasks", Mock(TaskContainer))
        registry.mutate { it.path "tasks" node basicAction }
        registry.mutate { it.path "tasks" node redundantAction }

        when:
        new RealizableTaskCollection(BasicTask, Mock(DefaultTaskCollection), project).realizeRuleTaskTypes()

        then:
        events == ['created task tasks.basic']
    }

    private Action mutator(ModelRegistryHelper registry, events, task, String path) {
        Action mutatorAction = Mock(Action)
        mutatorAction.execute(_) >> { MutableModelNode node ->
            node.addLink(registry.creator(path) {
                it.unmanaged(task, { events << "created task $path" })
            }
            )
        }
        return mutatorAction
    }
}

class BasicTask extends AbstractTask {}

class RedundantTask extends AbstractTask {}
