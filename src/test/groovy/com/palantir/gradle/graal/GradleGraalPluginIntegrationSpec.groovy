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

class GradleGraalPluginIntegrationSpec extends IntegrationSpec {

    @Rule MockWebServer server = new MockWebServer()
    String fakeBaseUrl

    def setup() {
        fakeBaseUrl = String.format("http://localhost:%s/oracle/graal/releases/download/", server.getPort())

        directory("src/main/java/com/palantir/test")
        file("src/main/java/com/palantir/test/Main.java") << '''
            package com.palantir.test;

            public final class Main {
                public static final void main(String[] args) {
                    System.out.println("hello, world!");
                }
            }
        '''

        // on Windows the path contains backslashes, which need to be escaped in .properties files
        def cacheDirPath = getProjectDir().toPath().resolve("cacheDir").toAbsolutePath().toString().replace("\\", "\\\\")
        file('gradle.properties') << "com.palantir.graal.cache.dir=${cacheDirPath}"
    }

    // there is no RC version for Windows
    @IgnoreIf({ Platform.operatingSystem() == Platform.OperatingSystem.WINDOWS })
    def 'allows specifying different RC graal version'() {
        setup:
        buildFile << """
            apply plugin: 'com.palantir.graal'

            graal {
               graalVersion '1.0.0-rc3'
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

        // requestUrl can contain "127.0.0.1" instead of "localhost"
        server.takeRequest().requestUrl.toString() =~ "http://(localhost|127\\.0\\.0\\.1):${server.port}" +
                "/oracle/graal/releases/download//vm-1.0.0-rc3/graalvm-ce-1.0.0-rc3-(macos|linux)-amd64.tar.gz"

        file("cacheDir/1.0.0-rc3/8/graalvm-ce-java8-1.0.0-rc3-amd64.tar.gz").text == '<<tgz>>'
    }

    // for Windows the download is a .zip, this is tested below
    @IgnoreIf({ Platform.operatingSystem() == Platform.OperatingSystem.WINDOWS })
    def 'allows specifying different GA graal version (non-windows)'() {
        setup:
        buildFile << """
            apply plugin: 'com.palantir.graal'

            graal {
               graalVersion '19.0.0'
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
          "/oracle/graal/releases/download//vm-19.0.0/graalvm-ce-(darwin|linux)-amd64-19.0.0.tar.gz"

        file("cacheDir/19.0.0/8/graalvm-ce-java8-19.0.0-amd64.tar.gz").text == '<<tgz>>'
    }

    @Requires({ Platform.operatingSystem() == Platform.OperatingSystem.WINDOWS })
    def 'allows specifying different GA graal version (windows)'() {
        setup:
        buildFile << """
            apply plugin: 'com.palantir.graal'

            graal {
               graalVersion '19.0.0'
               downloadBaseUrl '${fakeBaseUrl}'
            }
        """
        server.enqueue(new MockResponse().setBody('<<zip>>'))

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
                "/oracle/graal/releases/download//vm-19.0.0/graalvm-ce-windows-amd64-19.0.0.zip"

        file("cacheDir/19.0.0/8/graalvm-ce-java8-19.0.0-amd64.zip").text == '<<zip>>'
    }

    // for Windows the download is a .zip, this is tested below
    @IgnoreIf({ Platform.operatingSystem() == Platform.OperatingSystem.WINDOWS })
    def 'allows specifying GA graal version Java 8 19.3+ (non-windows)'() {
        setup:
        buildFile << """
            apply plugin: 'com.palantir.graal'

            graal {
               graalVersion '19.3.0'
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
                "oracle/graal/releases/download//vm-19.3.0/graalvm-ce-java8-(darwin|linux)-amd64-19.3.0.tar.gz"

        file("cacheDir/19.3.0/8/graalvm-ce-java8-19.3.0-amd64.tar.gz").text == '<<tgz>>'
    }

    @Requires({ Platform.operatingSystem() == Platform.OperatingSystem.WINDOWS })
    def 'allows specifying GA graal version Java 8 19.3+ (windows)'() {
        setup:
        buildFile << """
            apply plugin: 'com.palantir.graal'

            graal {
               graalVersion '19.3.0'
               downloadBaseUrl '${fakeBaseUrl}'
            }
        """
        server.enqueue(new MockResponse().setBody('<<zip>>'))

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
                "oracle/graal/releases/download//vm-19.3.0/graalvm-ce-java8-windows-amd64-19.3.0.zip"

        file("cacheDir/19.3.0/8/graalvm-ce-java8-19.3.0-amd64.zip").text == '<<zip>>'
    }

    // for Windows the download is a .zip, this is tested below
    @IgnoreIf({ Platform.operatingSystem() == Platform.OperatingSystem.WINDOWS })
    def 'allows specifying GA graal version Java 11 19.3+ (non-windows)'() {
        setup:
        buildFile << """
            apply plugin: 'com.palantir.graal'

            graal {
               graalVersion '19.3.0'
               javaVersion '11'
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
                "oracle/graal/releases/download//vm-19.3.0/graalvm-ce-java11-(darwin|linux)-amd64-19.3.0.tar.gz"

        file("cacheDir/19.3.0/11/graalvm-ce-java11-19.3.0-amd64.tar.gz").text == '<<tgz>>'
    }

    @Requires({ Platform.operatingSystem() == Platform.OperatingSystem.WINDOWS })
    def 'allows specifying GA graal version Java 11 19.3+ (windows)'() {
        setup:
        buildFile << """
            apply plugin: 'com.palantir.graal'

            graal {
               graalVersion '19.3.0'
               javaVersion '11'
               downloadBaseUrl '${fakeBaseUrl}'
            }
        """
        server.enqueue(new MockResponse().setBody('<<zip>>'))

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
                "oracle/graal/releases/download//vm-19.3.0/graalvm-ce-java11-windows-amd64-19.3.0.zip"

        file("cacheDir/19.3.0/11/graalvm-ce-java11-19.3.0-amd64.zip").text == '<<zip>>'
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
