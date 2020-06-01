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

import com.palantir.gradle.graal.util.JavaVersionUtil
import nebula.test.IntegrationSpec
import nebula.test.functional.ExecutionResult
import spock.lang.IgnoreIf
import spock.lang.Requires

import static com.palantir.gradle.graal.Platform.OperatingSystem.LINUX
import static com.palantir.gradle.graal.Platform.OperatingSystem.MAC
import static com.palantir.gradle.graal.Platform.OperatingSystem.WINDOWS

class GradleGraalEndToEndSpec extends IntegrationSpec {

    @Requires({ JavaVersionUtil.runtimeMajorVersion() == 8 })
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
        }
        '''

        when:
        ExecutionResult result = runTasksSuccessfully('nativeImage') // note, this accesses your real ~/.gradle cache
        println "Gradle Standard Out:\n" + result.standardOutput
        println "Gradle Standard Error:\n" + result.standardError
        def outputPath = "build/graal/hello-world"
        if (Platform.operatingSystem() == WINDOWS) {
            outputPath += ".exe"
        }
        File output = new File(getProjectDir(), outputPath)

        then:
        output.exists()
        output.getAbsolutePath().execute().text.equals("hello, world!" + System.lineSeparator())

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
        output.getAbsolutePath().execute().text.equals("hello, world (modified)!" + System.lineSeparator())
    }

    @Requires({ JavaVersionUtil.runtimeMajorVersion() == 8 })
    def 'test version 19.3.0 nativeImage'() {
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
            graalVersion '19.3.0'
        }
        '''

        when:
        ExecutionResult result = runTasksSuccessfully('nativeImage') // note, this accesses your real ~/.gradle cache
        println "Gradle Standard Out:\n" + result.standardOutput
        println "Gradle Standard Error:\n" + result.standardError
        def outputPath = "build/graal/hello-world"
        if (Platform.operatingSystem() == WINDOWS) {
            outputPath += ".exe"
        }
        File output = new File(getProjectDir(), outputPath)

        then:
        output.exists()
        output.getAbsolutePath().execute().text.equals("hello, world!" + System.lineSeparator())

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
        output.getAbsolutePath().execute().text.equals("hello, world (modified)!" + System.lineSeparator())
    }

    @Requires({ JavaVersionUtil.runtimeMajorVersion() == 8 })
    def 'test version 20.1.0 nativeImage'() {
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
            graalVersion '20.1.0'
        }
        '''

        when:
        ExecutionResult result = runTasksSuccessfully('nativeImage') // note, this accesses your real ~/.gradle cache
        println "Gradle Standard Out:\n" + result.standardOutput
        println "Gradle Standard Error:\n" + result.standardError
        def outputPath = "build/graal/hello-world"
        if (Platform.operatingSystem() == WINDOWS) {
            outputPath += ".exe"
        }
        File output = new File(getProjectDir(), outputPath)

        then:
        output.exists()
        output.getAbsolutePath().execute().text.equals("hello, world!" + System.lineSeparator())

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
        output.getAbsolutePath().execute().text.equals("hello, world (modified)!" + System.lineSeparator())
    }

    @Requires({ JavaVersionUtil.runtimeMajorVersion() == 11 })
    def 'test version 19.3.0 nativeImage Java 11'() {
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
            graalVersion '19.3.0'
            javaVersion '11'
        }
        '''

        when:
        ExecutionResult result = runTasksSuccessfully('nativeImage') // note, this accesses your real ~/.gradle cache
        println "Gradle Standard Out:\n" + result.standardOutput
        println "Gradle Standard Error:\n" + result.standardError
        def outputPath = "build/graal/hello-world"
        if (Platform.operatingSystem() == WINDOWS) {
            outputPath += ".exe"
        }
        File output = new File(getProjectDir(), outputPath)

        then:
        output.exists()
        output.getAbsolutePath().execute().text.equals("hello, world!" + System.lineSeparator())

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
        output.getAbsolutePath().execute().text.equals("hello, world (modified)!" + System.lineSeparator())
    }

    // there is no RC version for Windows
    @Requires({ JavaVersionUtil.runtimeMajorVersion() == 8 })
    @IgnoreIf({ Platform.operatingSystem() == WINDOWS })
    def 'test 1.0.0-rc5 nativeImage'() {
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
        def outputPath = "build/graal/hello-world"
        if (Platform.operatingSystem() == Platform.OperatingSystem.WINDOWS) {
            outputPath += ".exe"
        }
        File output = new File(getProjectDir(), outputPath)

        then:
        output.exists()
        output.getAbsolutePath().execute().text.equals("hello, world!" + System.lineSeparator())

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
        output.getAbsolutePath().execute().text.equals("hello, world (modified)!" + System.lineSeparator())
    }

    @Requires({ JavaVersionUtil.runtimeMajorVersion() == 8 })
    def 'allows specifying additional properties on default version'() {
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
            graalVersion '19.1.0'
            // By default, only file:// is supported, see https://github.com/oracle/graal/blob/master/substratevm/URL-PROTOCOLS.md
            option '-H:EnableURLProtocols=http'
        }
        '''

        when:
        ExecutionResult result = runTasks('nativeImage') // note, this accesses your real ~/.gradle cache
        println "Gradle Standard Out:\n" + result.standardOutput
        println "Gradle Standard Error:\n" + result.standardError
        def outputPath = "build/graal/hello-world"
        if (Platform.operatingSystem() == WINDOWS) {
            outputPath += ".exe"
        }
        File output = new File(getProjectDir(), outputPath)

        then:
        output.exists()
        output.getAbsolutePath().execute().text.toLowerCase().contains("<html")
    }

    @Requires({ JavaVersionUtil.runtimeMajorVersion() == 8 })
    def 'can build shared libraries on default version'() {
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
        }
        '''

        when:
        runTasksSuccessfully('sharedLibrary')
        File dylibFile = new File(getProjectDir(), "build/graal/hello-world." + getSharedLibPrefixByOs())

        then:
        dylibFile.exists()
    }

    @Requires({ JavaVersionUtil.runtimeMajorVersion() == 8 })
    def 'can build shared libraries on version 19.3.0'() {
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
            graalVersion '19.3.0'
        }
        '''

        when:
        runTasksSuccessfully('sharedLibrary')
        File dylibFile = new File(getProjectDir(), "build/graal/hello-world." + getSharedLibPrefixByOs())

        then:
        dylibFile.exists()
    }

    @Requires({ JavaVersionUtil.runtimeMajorVersion() == 11 })
    def 'can build shared libraries on version 19.3.0 Java 11'() {
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
            graalVersion '19.3.0'
            javaVersion '11'
        }
        '''

        when:
        runTasksSuccessfully('sharedLibrary')
        File dylibFile = new File(getProjectDir(), "build/graal/hello-world." + getSharedLibPrefixByOs())

        then:
        dylibFile.exists()
    }

    // there is no RC version for Windows
    @Requires({ JavaVersionUtil.runtimeMajorVersion() == 8 })
    @IgnoreIf({ Platform.operatingSystem() == WINDOWS })
    def 'can build shared libraries on 1.0.0-rc5'() {
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

    @Requires({ JavaVersionUtil.runtimeMajorVersion() == 8 })
    def 'should not allow empty mainClass on nativeImage'() {
        buildFile << '''
        apply plugin: 'com.palantir.graal'

        graal {            
            outputName 'hello-world'
            graalVersion '19.1.0'
            option '-H:EnableURLProtocols=http'            
        }
        '''

        when:
        ExecutionResult result = runTasksWithFailure('nativeImage') // note, this accesses your real ~/.gradle cache
        println "Gradle Standard Out:\n" + result.standardOutput
        println "Gradle Standard Error:\n" + result.standardError

        then:
        result.standardError.contains("No value has been specified for property 'mainClass'")
    }

    @Requires({ JavaVersionUtil.runtimeMajorVersion() == 8 })
    def 'should not allow to add -H:Name'() {
        buildFile << '''
        apply plugin: 'com.palantir.graal'

        graal {
            mainClass 'com.palantir.test.Main'
            outputName 'hello-world'
            graalVersion '19.1.0'
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

    def getSharedLibPrefixByOs() {
        switch (Platform.operatingSystem()) {
            case MAC:
                return "dylib"
            case LINUX:
                return "so"
            case WINDOWS:
                return "dll"
            default:
                throw new IllegalStateException("No GraalVM support for " + Platform.operatingSystem())
        }
    }

}
