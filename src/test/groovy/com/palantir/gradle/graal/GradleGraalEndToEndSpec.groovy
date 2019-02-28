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
import static com.palantir.gradle.graal.Platform.OperatingSystem.LINUX
import static com.palantir.gradle.graal.Platform.OperatingSystem.MAC

class GradleGraalEndToEndSpec extends IntegrationSpec {

    def 'test default version nativeImage'() {
        setup:
        directory("src/main/java/com/palantir/test")
        file("src/main/java/com/palantir/test/Main.java") << '''
        package com.palantir.test;
       
        public final class Main {
            public static final void main(String[] args) {
                System.out.println("hello, world!");
            }
        }
        '''

        buildFile << '''
        apply plugin: 'com.palantir.graal'

        graal {
            mainClass 'com.palantir.test.Main'
            outputName 'hello-world'
            graalVersion '1.0.0-rc5'
        }
        '''

        when:
        ExecutionResult result = runTasksSuccessfully('nativeImage') // note, this accesses your real ~/.gradle cache
        println "Gradle Standard Out:\n" + result.standardOutput
        println "Gradle Standard Error:\n" + result.standardError
        File output = new File(getProjectDir(), "build/graal/hello-world");

        then:
        output.exists()
        output.getAbsolutePath().execute().text.equals("hello, world!\n")

        when:
        ExecutionResult result2 = runTasksSuccessfully('nativeImage')

        then:
        result2.wasUpToDate(':nativeImage')

        when:
        new File(getProjectDir(), "src/main/java/com/palantir/test/Main.java").text = '''
        package com.palantir.test;
        
        public final class Main {
            public static final void main(String[] args) {
                System.out.println("hello, world (modified)!");
            }
        }
        '''
        ExecutionResult result3 = runTasksSuccessfully('nativeImage')

        then:
        println result3.standardOutput
        !result3.wasUpToDate(':nativeImage')
        output.getAbsolutePath().execute().text.equals("hello, world (modified)!\n")
    }

    def 'allows specifying additional properties'() {
        setup:
        directory("src/main/java/com/palantir/test")
        file("src/main/java/com/palantir/test/Main.java") << '''
        package com.palantir.test;
       
        import java.io.IOException;
        import java.net.URL;
        
        public final class Main {
          public static final void main(String[] args) throws IOException {
            String result = convertStreamToString(new URL("http://www.google.com/").openStream());
            System.out.println(result);       
          }
        
          static String convertStreamToString(java.io.InputStream is) {
            java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\\\A");
            return s.hasNext() ? s.next() : "";
          }
        }
        '''

        buildFile << '''
        apply plugin: 'com.palantir.graal'

        graal {
            mainClass 'com.palantir.test.Main'
            outputName 'hello-world'
            graalVersion '1.0.0-rc5'
            // By default, only file:// is supported, see https://github.com/oracle/graal/blob/master/substratevm/URL-PROTOCOLS.md
            option '-H:EnableURLProtocols=http'
        }
        '''

        when:
        ExecutionResult result = runTasks('nativeImage') // note, this accesses your real ~/.gradle cache
        println "Gradle Standard Out:\n" + result.standardOutput
        println "Gradle Standard Error:\n" + result.standardError
        File output = new File(getProjectDir(), "build/graal/hello-world");

        then:
        output.exists()
        output.getAbsolutePath().execute().text.toLowerCase().contains("<html")
    }


    def 'can build shared libraries'() {
        setup:
        directory("src/main/java/com/palantir/test")
        file("src/main/java/com/palantir/test/Main.java") << '''
        package com.palantir.test;
        public final class Main {}
        '''
        buildFile << '''
        apply plugin: 'com.palantir.graal'
        graal {
            outputName 'hello-world'
            graalVersion '1.0.0-rc5'
        }
        '''

        when:
        runTasksSuccessfully('sharedLibrary')
        File dylibFile = new File(getProjectDir(), "build/graal/hello-world." + getSharedLibPrefixByOs())

        then:
        dylibFile.exists()
    }

    def getSharedLibPrefixByOs() {
        switch (Platform.operatingSystem()) {
            case MAC:
                return "dylib"
            case LINUX:
                return "so"
            default:
                throw new IllegalStateException("No GraalVM support for " + Platform.operatingSystem())
        }
    }

    def 'should not allow to add -H:Name'() {
        buildFile << '''
        apply plugin: 'com.palantir.graal'

        graal {
            mainClass 'com.palantir.test.Main'
            outputName 'hello-world'
            graalVersion '1.0.0-rc5'
            option '-H:EnableURLProtocols=http'
            option '-H:Name=foo'
        }
        '''

        when:
        ExecutionResult result = runTasksWithFailure('nativeImage') // note, this accesses your real ~/.gradle cache
        println "Gradle Standard Out:\n" + result.standardOutput
        println "Gradle Standard Error:\n" + result.standardError

        then:
        result.standardError.contains("Use 'outputName' instead of")
    }
}
