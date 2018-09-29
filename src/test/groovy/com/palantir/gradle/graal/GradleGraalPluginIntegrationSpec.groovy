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

import java.nio.file.Files
import java.nio.file.Path
import nebula.test.IntegrationSpec
import nebula.test.functional.ExecutionResult
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.gradle.util.GFileUtils
import org.junit.Rule

class GradleGraalPluginIntegrationSpec extends IntegrationSpec {

    @Rule MockWebServer server = new MockWebServer()
    String fakeBaseUrl
    Path cacheDir

    def setup() {
        cacheDir = Files.createDirectory(getProjectDir().toPath().resolve("cacheDir"))
        GFileUtils.cleanDirectory(cacheDir.toFile())

        fakeBaseUrl = String.format("http://localhost:%s/oracle/graal/releases/download/", server.getPort())

        new File(getProjectDir(), "src/main/java/com/palantir/test").mkdirs()
        new File(getProjectDir(), "src/main/java/com/palantir/test/Main.java") << '''
            package com.palantir.test;

            public final class Main {
                public static final void main(String[] args) {
                    System.out.println("hello, world!");
                }
            }
        '''
    }

    def 'allows specifying different graal version'() {
        setup:
        buildFile << """
            apply plugin: 'com.palantir.graal'

            graal {
               graalVersion '1.0.0-rc3'
               downloadBaseUrl '${fakeBaseUrl}'
            }
        """
        server.enqueue(new MockResponse().setBody('<<tgz>>'));

        when:
        ExecutionResult result = runTasksSuccessfully('downloadGraalTooling', "-Dcom.palantir.graal.cache.dir=${cacheDir}")

        then:
        result.wasExecuted(':downloadGraalTooling')
        result.wasUpToDate(':downloadGraalTooling') == false
        result.wasSkipped(':downloadGraalTooling') == false

        server.takeRequest().requestUrl.toString() =~ "http://localhost:${server.port}" +
                "/oracle/graal/releases/download//vm-1.0.0-rc3/graalvm-ce-1.0.0-rc3-(macos|linux)-amd64.tar.gz"
        file("cacheDir/1.0.0-rc3/graalvm-ce-1.0.0-rc3-amd64.tar.gz").text == '<<tgz>>'
    }

    def 'downloadGraalTooling behaves incrementally'() {
        setup:
        buildFile << """
            apply plugin: 'com.palantir.graal'

            graal {
               downloadBaseUrl '${fakeBaseUrl}'
            }
        """
        server.enqueue(new MockResponse().setBody('<<tgz>>'));

        when:
        ExecutionResult result1 = runTasksSuccessfully('downloadGraalTooling', "-Dcom.palantir.graal.cache.dir=${cacheDir}")
        ExecutionResult result2 = runTasksSuccessfully('downloadGraalTooling', "-Dcom.palantir.graal.cache.dir=${cacheDir}")

        then:
        result1.wasSkipped(':downloadGraalTooling') == false
        result2.wasSkipped(':downloadGraalTooling') == true
    }
}
