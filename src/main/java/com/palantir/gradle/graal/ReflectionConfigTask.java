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

import groovy.json.JsonOutput;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

public class ReflectionConfigTask extends DefaultTask {

    private final RegularFileProperty file = newOutputFile();
    private final ListProperty<String> classes = getProject().getObjects().listProperty(String.class);

    public ReflectionConfigTask() {
        setGroup(GradleGraalPlugin.TASK_GROUP);
        setDescription("Generates a reflectconfig.json file based on supplied class names");

        file.set(new File(getProject().getBuildDir(), "graal/reflectconfig.json"));
        onlyIf(t -> classes.isPresent() && !classes.get().isEmpty());
    }

    @TaskAction
    public final void generateFile() throws IOException {
        List<Map<String, Object>> json = classes.get().stream()
                .map(clazz -> {
                    LinkedHashMap<String, Object> map = new LinkedHashMap<>();
                    map.put("name", clazz);
                    map.put("allDeclaredConstructors", true);
                    map.put("allPublicConstructors", true);
                    map.put("allDeclaredMethods", true);
                    map.put("allPublicMethods", true);
                    map.put("allDeclaredFields", true);
                    map.put("allPublicFields", true);
                    return map;
                })
                .collect(toList());

        String string = JsonOutput.prettyPrint(JsonOutput.toJson(json));
        Files.write(file.get().getAsFile().toPath(), string.getBytes(StandardCharsets.UTF_8));
    }

    @Input
    public final Provider<List<String>> getClasses() {
        return classes;
    }

    @OutputFile
    public final RegularFileProperty getOutputJsonFile() {
        return file;
    }

    public final void setFile(File file) {
        this.file.set(file);
    }

    public final void add(String string) {
        classes.add(string);
    }

    public final void add(Provider<String> value) {
        classes.add(value);
    }

    public final void addAll(String... strings) {
        classes.addAll(strings);
    }
}
