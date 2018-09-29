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

import static java.util.stream.Collectors.toList;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Stream;
import org.gradle.api.Project;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Provider;

/**
 * See substratevm/src/com.oracle.svm.core/src/com/oracle/svm/core/SubstrateOptions.java in
 * https://github.com/oracle/graal/.
 */
public final class NativeImageOptions implements Serializable {
    private static final long serialVersionUID = 9307652985066589L;

    // using Object to represent String | Provider<String>
    private final ListProperty<Object> list;

    public NativeImageOptions(Project project) {
        list = project.getObjects().listProperty(Object.class);
    }

    public List<String> get() {
        return getProvider().get();
    }

    public void set(String option) {
        list.add(option);
    }

    public void set(Provider<String> option) {
        list.add(option);
    }

    public Provider<List<String>> getProvider() {
        return list.map(providers -> providers.stream().flatMap(object -> {

            if (object instanceof Provider) {
                Provider provider = (Provider) object;
                return provider.isPresent() ? Stream.of((String) provider.get()) : Stream.empty();
            }

            if (object instanceof String) {
                return Stream.of((String) object);
            }

            throw new IllegalStateException("list elements must be either String or Provider<String>, was: " + object);
        }).collect(toList()));
    }
}
