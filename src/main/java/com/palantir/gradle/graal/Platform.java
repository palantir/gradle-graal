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

import java.util.Locale;
import java.util.Set;

/** Utility methods for keying on operating system architecture and native code tooling. */
// TODO(melliot): replace this with the Gradle-native implementations (see NativePlatform) once promoted from incubating
public final class Platform {

    public enum OperatingSystem {
        MAC,
        LINUX;
    }

    public enum Architecture {
        X86_64,
        AARCH64;
    }

    public static OperatingSystem operatingSystem() {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (os.contains("mac")) {
            return OperatingSystem.MAC;
        } else if (os.contains("linux")) {
            return OperatingSystem.LINUX;
        } else if (os.contains("win")) {
            throw new UnsupportedOperationException("Windows is not yet supported");
        }
        throw new UnsupportedOperationException("Cannot get operating system for " + os);
    }

    public static Architecture architecture() {
        String arch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);

        if (Set.of("x86_64", "x64", "amd64").contains(arch)) {
            return Architecture.X86_64;
        }

        if (Set.of("arm", "arm64", "aarch64").contains(arch)) {
            return Architecture.AARCH64;
        }

        throw new UnsupportedOperationException("Cannot get architecture for " + arch);
    }

    private Platform() {}
}
