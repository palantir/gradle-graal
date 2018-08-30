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

import java.io.File;
import java.nio.file.Path;
import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.internal.TaskInternal;
import org.gradle.api.provider.Provider;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

/** Extracts GraalVM tooling from downloaded tgz archive using the system's tar command. */
public class ExtractGraalTask extends DefaultTask {

    @Input private File inputTgz;
    @Input private Provider<String> graalVersion;

    @TaskAction
    public final void extractGraal() {
        if (!graalVersion.isPresent()) {
            throw new IllegalStateException("extract task requires graal.graalVersion to be defined.");
        }

        // ideally this would be a CopyTask, but through Gradle 4.9 CopyTask fails to correctly extract symlinks
        getProject().exec(spec -> {
            spec.executable("tar");
            spec.args("-xzf", inputTgz.getAbsolutePath());
            spec.workingDir(GradleGraalPlugin.CACHE_DIR.resolve(graalVersion.get()));
        });
    }

    @OutputDirectory
    public final Path getOutputDirectory() {
        return GradleGraalPlugin.CACHE_DIR.resolve(graalVersion.get()).resolve("graalvm-ce-" + graalVersion.get());
    }

    @Override
    public Spec<? super TaskInternal> getOnlyIf() {
        return spec -> !getOutputDirectory().toFile().exists();
    }

    @Override
    public final String getGroup() {
        return GradleGraalPlugin.TASK_GROUP;
    }

    @Override
    public final String getDescription() {
        return "Extracts GraalVM tooling from downloaded tgz archive using the system's tar command.";
    }

    @SuppressWarnings("checkstyle:hiddenfield")
    public final void configure(File inputTgz, Provider<String> graalVersion) {
        this.inputTgz = inputTgz;
        this.graalVersion = graalVersion;
    }
}
