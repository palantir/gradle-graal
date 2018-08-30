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

package com.palantir.gradle.graal;

/** Utility methods for keying on operating system architecture and native code tooling. */
// TODO(melliot): replace this with the Gradle-native implementations (see NativePlatform) once promoted from incubating
public final class Platform {

    public enum OperatingSystem {
        MAC,
        LINUX,
        WINDOWS,
        UNKNOWN
    }

    public enum Architecture {
        AMD64,
        UNKNOWN
    }

    public static OperatingSystem operatingSystem() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac")) {
            return OperatingSystem.MAC;
        } else if (os.contains("linux")) {
            return OperatingSystem.LINUX;
        } else if (os.contains("win")) {
            return OperatingSystem.WINDOWS;
        }
        return OperatingSystem.UNKNOWN;
    }

    public static Architecture architecture() {
        String arch = System.getProperty("os.arch");
        if (arch.contains("64")) {
            return Architecture.AMD64;
        }
        return Architecture.UNKNOWN;
    }

    private Platform() {}
}
