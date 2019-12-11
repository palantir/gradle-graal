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

public final class GraalVersionUtil {
    public static boolean isGraalVersionGreatherThan19_3(String graalVersion) {
        try {
            final String[] versionSplit = graalVersion.split("\\.", -1);
            final int majorVersion = Integer.valueOf(versionSplit[0]);
            final int minorVersion = Integer.valueOf(versionSplit[1]);
            return majorVersion > 19 || (majorVersion == 19 && minorVersion >= 3);
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private GraalVersionUtil() {}
}
