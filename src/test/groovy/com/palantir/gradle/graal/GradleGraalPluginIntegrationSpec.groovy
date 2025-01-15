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
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Rule
import spock.lang.IgnoreIf
import spock.lang.Requires

import java.nio.file.Path

class GradleGraalPluginIntegrationSpec extends IntegrationSpec {

    @Rule MockWebServer server = new MockWebServer()
    String fakeBaseUrl

    def setup() {
        fakeBaseUrl = String.format("http://localhost:%s/graalvm/graalvm-ce-builds/releases/download", server.getPort())

        directory("src/main/java/com/palantir/test")
        file("src/main/java/com/palantir/test/Main.java") << '''
            package com.palantir.test;

            public final class Main {
                public static final void main(String[] args) {
                    System.out.println("hello, world!");
                }
            }
        '''

        def cacheDirPath = getProjectDir().toPath().resolve("cacheDir").toAbsolutePath().toString()
        file('gradle.properties') << "com.palantir.graal.cache.dir=${cacheDirPath}"
    }

    def 'allows specifying different GA Graal version'() {
        setup:
        buildFile << """
            apply plugin: 'com.palantir.graal'

            graal {
               graalVersion '23.0.1'
               downloadBaseUrl '${fakeBaseUrl}'
            }
        """
        server.enqueue(new MockResponse().setBody('<<tgz>>'))

        when:
        ExecutionResult result = runTasksSuccessfully('downloadGraalTooling')

        then:
        println result.getStandardOutput()
        result.wasExecuted(':downloadGraalTooling')
        !result.wasUpToDate(':downloadGraalTooling')
        !result.wasSkipped(':downloadGraalTooling')

        // `requestUrl` can contain "127.0.0.1" instead of "localhost"
        // worse yet, it can contain any hostname that is defined for 127.0.0.1 in the hosts file
        // e.g. Docker Desktop puts "127.0.0.1 kubernetes.docker.internal" in there, which ends up in `requestUrl`
        // so the comparison is only made for `path`
        server.takeRequest().path =~
          "/graalvm/graalvm-ce-builds/releases/download/jdk-23.0.1/graalvm-community-jdk-23.0.1_(macos|linux)-(aarch64|x64)_bin.tar.gz"

        file(String.format("cacheDir/23.0.1/%s", Path.of(server.takeRequest().path).getFileName().toString())).text == '<<tgz>>'
    }

    def 'downloadGraalTooling behaves incrementally'() {
        setup:
        buildFile << """
            apply plugin: 'com.palantir.graal'

            graal {
               downloadBaseUrl '${fakeBaseUrl}'
            }
        """
        server.enqueue(new MockResponse().setBody('<<tgz>>'))

        when:
        ExecutionResult result1 = runTasksSuccessfully('downloadGraalTooling')
        ExecutionResult result2 = runTasksSuccessfully('downloadGraalTooling')

        then:
        result1.wasSkipped(':downloadGraalTooling') == false
        result2.wasSkipped(':downloadGraalTooling') == true
    }
}
