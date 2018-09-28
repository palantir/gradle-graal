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
import org.junit.ClassRule
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class GradleGraalPluginIntegrationSpec extends IntegrationSpec {

    @Rule
    TemporaryFolder folder = new TemporaryFolder();

    @ClassRule
    static MockWebServer server = new MockWebServer()
    static String fakeBaseUrl

    def setupSpec() {
        fakeBaseUrl = String.format("http://localhost:%s/oracle/graal/releases/download/", server.getPort())
    }

    def setup() {
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

//    def 'test default version nativeImage'() {
//        setup:
//        buildFile << '''
//            apply plugin: 'java'
//            apply plugin: 'com.palantir.graal'
//
//            graal {
//               mainClass 'com.palantir.test.Main'
//               outputName 'hello-world'
//            }
//        '''
//
//        when:
//        ExecutionResult result = runTasksSuccessfully('nativeImage')
//        println "Gradle Standard Out:\n" + result.standardOutput
//        println "Gradle Standard Error:\n" + result.standardError
//        File output = new File(getProjectDir(), "build/graal/hello-world");
//
//        then:
//        output.exists()
//        output.getAbsolutePath().execute().text.equals("hello, world!\n")
//    }

    def 'allows specifying different graal version'() {
        setup:
        buildFile << """
            apply plugin: 'java'
            apply plugin: 'com.palantir.graal'

            graal {
               graalVersion '1.0.0-rc5'
               downloadBaseUrl '${fakeBaseUrl}'
            }
        """
        server.enqueue(new MockResponse().setBody('<<tgz>>'));

        when:
        runTasksSuccessfully('downloadGraalTooling', "-Duser.home=${folder.getRoot()}")

        then:
        new File(folder.getRoot(),
                ".gradle/caches/com.palantir.graal/1.0.0-rc5/graalvm-ce-1.0.0-rc5-amd64.tar.gz").text == '<<tgz>>'
        server.takeRequest().requestUrl.toString() =~ "http://localhost:${server.port}" +
                "/oracle/graal/releases/download//vm-1.0.0-rc5/graalvm-ce-1.0.0-rc5-(macos|linux)-amd64.tar.gz"
    }

    def 'downloadGraalTooling behaves incrementally'() {
        setup:
        buildFile << """
            apply plugin: 'java'
            apply plugin: 'com.palantir.graal'

            graal {
               graalVersion '1.0.0-rc5'
               downloadBaseUrl '${fakeBaseUrl}'
            }
        """
        server.enqueue(new MockResponse().setBody('<<tgz>>'));

        when:
        ExecutionResult result1 = runTasksSuccessfully('downloadGraalTooling', "-Duser.home=${folder.getRoot()}")
        ExecutionResult result2 = runTasksSuccessfully('downloadGraalTooling', "-Duser.home=${folder.getRoot()}")

        then:
        result1.wasUpToDate(':downloadGraalTooling') == false
        result2.wasUpToDate(':downloadGraalTooling') == true
    }
}
