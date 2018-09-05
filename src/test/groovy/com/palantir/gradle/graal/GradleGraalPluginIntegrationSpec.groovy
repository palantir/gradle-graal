/*
 * (c) Copyright 2018 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.gradle.graal

import nebula.test.IntegrationSpec
import nebula.test.functional.ExecutionResult

class GradleGraalPluginIntegrationSpec extends IntegrationSpec {
    def 'test default version nativeImage'() {
        setup:
        new File(getProjectDir(), "src/main/java/com/palantir/test").mkdirs()
        new File(getProjectDir(), "src/main/java/com/palantir/test/Main.java") << '''
            package com.palantir.test;

            public final class Main {
                public static final void main(String[] args) {
                    System.out.println("hello, world!");
                }
            }
        '''

        buildFile << '''
            apply plugin: 'java'
            apply plugin: 'com.palantir.graal'

            graal {
               mainClass 'com.palantir.test.Main'
               outputName 'hello-world'
            }
        '''

        when:
        ExecutionResult result = runTasks('nativeImage')
        // capture output from Gradle runs
        println "Gradle Standard Out:\n" + result.standardOutput
        println "Gradle Standard Error:\n" + result.standardError
        File output = new File(getProjectDir(), "build/graal/hello-world");

        then:
        result.success
        output.exists()
        output.getAbsolutePath().execute().text.equals("hello, world!\n")
    }
}
