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
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

/** Extracts GraalVM tooling from downloaded tgz archive using the system's tar command. */
public class ExtractGraalTask extends DefaultTask {

    private final Property<File> inputTgz = getProject().getObjects().property(File.class);
    private final Provider<String> graalVersion;

    @Inject
    public ExtractGraalTask(GraalExtension extension) {
        onlyIf(task -> !getOutputDirectory().toFile().exists());
        setGroup(GradleGraalPlugin.TASK_GROUP);
        setDescription("Extracts GraalVM tooling from downloaded tgz archive using the system's tar command.");

        graalVersion = extension.getGraalVersion();
    }

    @InputFile
    public final Provider<File> getInputTgz() {
        return inputTgz;
    }

    public final void setInputTgz(Provider<File> value) {
        this.inputTgz.set(value);
    }

    @Input
    public final Provider<String> getGraalVersion() {
        return graalVersion;
    }

    @TaskAction
    public final void extractGraal() {
        if (!graalVersion.isPresent()) {
            throw new IllegalStateException("extract task requires graal.graalVersion to be defined.");
        }

        // ideally this would be a CopyTask, but through Gradle 4.9 CopyTask fails to correctly extract symlinks
        getProject().exec(spec -> {
            spec.executable("tar");
            spec.args("-xzf", inputTgz.get().getAbsolutePath());
            spec.workingDir(GradleGraalPlugin.CACHE_DIR.resolve(graalVersion.get()));
        });
    }

    @OutputDirectory
    public final Path getOutputDirectory() {
        return GradleGraalPlugin.CACHE_DIR.resolve(graalVersion.get()).resolve("graalvm-ce-" + graalVersion.get());
    }
}
