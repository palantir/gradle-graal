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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.tasks.Jar;

/**
 * Adds tasks to download, extract and interact with GraalVM tooling.
 *
 * <p>All tooling execution (e.g. nativeImage) will cause GraalVM tooling to download and cache if not already
 * present. Currently, GraalVM CE only supports MacOS and Linux, and, as a result, this plugin will only correctly
 * function on MacOS and Linux. The plugin will automatically select the correct architecture and error clearly
 * if the runtime architecture is not supported.</p>
 *
 * <p>Downloads are stored in ~/.gradle/caches/com.palantir.graal using the following structure:</p>
 * <pre>
 * ~/.gradle/caches/com.palantir.graal/
 * └── [version]/
 *     ├── graalvm-ce-[version]/
 *     │   └── [local architecture-specific GraalVM tooling]
 *     └── graalvm-ce-[version]-amd64.tar.gz
 * </pre>
 */
public class GradleGraalPlugin implements Plugin<Project> {

    /** Location to cache downloaded and extracted GraalVM tooling. */
    static final Path CACHE_DIR = Optional.ofNullable(System.getProperty("com.palantir.graal.cache.dir"))
            .map(Paths::get)
            .orElse(Paths.get(System.getProperty("user.home"), ".gradle", "caches", "com.palantir.graal"));

    static final String TASK_GROUP = "Graal";

    @Override
    public final void apply(Project project) {
        project.getPluginManager().apply("java");
        GraalExtension extension = project.getExtensions().create("graal", GraalExtension.class, project);

        TaskProvider<DownloadGraalTask> downloadGraal = project.getTasks().register(
                "downloadGraalTooling",
                DownloadGraalTask.class,
                task -> {
                    task.setGraalVersion(extension.getGraalVersion());
                    task.setDownloadBaseUrl(extension.getDownloadBaseUrl());
                });

        TaskProvider<ExtractGraalTask> extractGraal = project.getTasks().register(
                "extractGraalTooling",
                ExtractGraalTask.class,
                task -> {
                    task.setGraalVersion(extension.getGraalVersion());
                    task.setInputTgz(downloadGraal.get().getTgz());
                    task.dependsOn(downloadGraal);
                });

        TaskProvider<Jar> jar = project.getTasks().withType(Jar.class).named("jar");
        TaskProvider<NativeImageTask> nativeImage = project.getTasks().register(
                "nativeImage",
                NativeImageTask.class,
                project.getConfigurations().named("runtimeClasspath"));
        nativeImage.configure(task -> {
            task.setMainClass(extension.getMainClass());
            task.setOutputName(extension.getOutputName());
            task.setGraalVersion(extension.getGraalVersion());
            task.dependsOn(extractGraal);
            task.setJarFile(jar.map(j -> j.getOutputs().getFiles().getSingleFile()));
            task.dependsOn(jar);
        });
    }
}
