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

import org.gradle.api.DefaultTask;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

/** Extracts GraalVM tooling from downloaded tgz archive using the system's tar command. */
public class ExtractGraalTask extends DefaultTask {

    private final RegularFileProperty inputTgz = newInputFile();
    private final Property<String> graalVersion = getProject().getObjects().property(String.class);
    private final DirectoryProperty outputDirectory = newOutputDirectory();

    public ExtractGraalTask() {
        setGroup(GradleGraalPlugin.TASK_GROUP);
        setDescription("Extracts GraalVM tooling from downloaded tgz archive using the system's tar command.");

        onlyIf(task -> !getOutputDirectory().get().getAsFile().exists());
        outputDirectory.set(graalVersion.map(v ->
                getProject().getLayout().getProjectDirectory()
                        .dir(GradleGraalPlugin.CACHE_DIR.toFile().getAbsolutePath())
                        .dir(v)
                        .dir("graalvm-ce-" + v)));
    }

    @TaskAction
    public final void extractGraal() {
        if (!graalVersion.isPresent()) {
            throw new IllegalStateException("extract task requires graal.graalVersion to be defined.");
        }

        // ideally this would be a CopyTask, but through Gradle 4.9 CopyTask fails to correctly extract symlinks
        getProject().exec(spec -> {
            spec.executable("tar");
            spec.args("-xzf", inputTgz.get().getAsFile().getAbsolutePath());
            spec.workingDir(GradleGraalPlugin.CACHE_DIR.resolve(graalVersion.get()));
        });
    }

    @InputFile
    public final Provider<RegularFile> getInputTgz() {
        return inputTgz;
    }

    public final void setInputTgz(Provider<RegularFile> value) {
        this.inputTgz.set(value);
    }

    @Input
    public final Provider<String> getGraalVersion() {
        return graalVersion;
    }

    public final void setGraalVersion(Provider<String> provider) {
        graalVersion.set(provider);
    }

    @OutputDirectory
    public final Provider<Directory> getOutputDirectory() {
        return outputDirectory;
    }
}
