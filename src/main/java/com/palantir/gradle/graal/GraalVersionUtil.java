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
    public static boolean isGraalVersionGreaterOrEqualThan(String graalVersion, int majorVersion, int minorVersion) {
        try {
            final String[] versionSplit = graalVersion.split("\\.", -1);
            final int majorVersion0 = Integer.valueOf(versionSplit[0]);
            final int minorVersion0 = Integer.valueOf(versionSplit[1]);
            return majorVersion0 > majorVersion || (majorVersion0 == majorVersion && minorVersion0 >= minorVersion);
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    public static boolean isGraalRcVersion(String graalVersion) {
        return graalVersion.startsWith("1.0.0-rc");
    }

    public static boolean isGraalDevVersion(String graalVersion) {
        return graalVersion.contains("-dev-");
    }

    // 22.1.0-dev-20220314_2252 -> 22.1.0-dev
    public static String cutDevSignature(String graalVersion) {
        return graalVersion.substring(0, graalVersion.indexOf("-dev") + 4);
    }

    private GraalVersionUtil() {}
}
