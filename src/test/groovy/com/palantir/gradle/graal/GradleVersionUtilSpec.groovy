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

import nebula.test.ProjectSpec
import org.gradle.api.GradleException
import spock.lang.Specification

class GradleVersionUtilSpec extends Specification {
    def 'should detect Graal version 19.2.0 is not 19.3+'() {
        expect:
        !GraalVersionUtil.isGraalVersionGreaterOrEqualThan("19.2.0", 19, 3)
    }

    def 'should detect Graal version 19.3.0 is 19.3+'() {
        expect:
        GraalVersionUtil.isGraalVersionGreaterOrEqualThan("19.3.0", 19, 3)
    }

    def 'should detect Graal version empty is not 19.3+'() {
        expect:
        !GraalVersionUtil.isGraalVersionGreaterOrEqualThan("", 19, 3)
    }

    def 'should detect Graal version 21.1.0 is 21.1+'() {
        expect:
        GraalVersionUtil.isGraalVersionGreaterOrEqualThan("21.1.0", 21, 1)
    }

    def 'should detect Graal version 21.0.0 is not 21.1+'() {
        expect:
        !GraalVersionUtil.isGraalVersionGreaterOrEqualThan("21.0.0", 21, 1)
    }
}
