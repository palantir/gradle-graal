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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

record CommandSpec(String executable, List<String> args) {
    CommandSpec(String executable) {
        this(executable, new ArrayList<>());
    }

    void addArg(String arg) {
        args.add(arg);
    }

    void addArgs(List<String> newArgs) {
        args.addAll(newArgs);
    }

    List<String> toCommand() {
        return Stream.concat(Stream.of(executable), args.stream()).toList();
    }
}
