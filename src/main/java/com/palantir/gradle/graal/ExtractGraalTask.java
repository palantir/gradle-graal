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
import java.nio.file.Paths;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
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

    private final RegularFileProperty inputArchive = getProject().getObjects().fileProperty();
    private final Property<String> graalVersion = getProject().getObjects().property(String.class);
    private final DirectoryProperty outputDirectory = getProject().getObjects().directoryProperty();
    private final Property<Path> cacheDir = getProject().getObjects().property(Path.class);
    private final Property<String> graalDirectoryName =
            getProject().getObjects().property(String.class);

    public ExtractGraalTask() {
        setGroup(GradleGraalPlugin.TASK_GROUP);
        setDescription("Extracts GraalVM tooling from downloaded archive using the system's tar command or Gradle's"
                + " copy method.");

        onlyIf(_task -> !getOutputDirectory().get().getAsFile().exists());
        outputDirectory.set(cacheDir.map(_cd -> getProject()
                .getLayout()
                .getProjectDirectory()
                .dir(cacheDir.get().toFile().getAbsolutePath())
                .dir(graalVersion.get())
                .dir(graalDirectoryName.get())));
    }

    @TaskAction
    public final void extractGraal() {
        if (!graalVersion.isPresent()) {
            throw new IllegalStateException("extract task requires graal.graalVersion to be defined.");
        }

        Project project = getProject();
        File inputArchiveFile = inputArchive.get().getAsFile();
        Path versionedCacheDir = cacheDir.get().resolve(graalVersion.get());

        if (inputArchiveFile.getName().endsWith(".zip")) {
            project.copy(copySpec -> {
                copySpec.from(project.zipTree(inputArchiveFile));
                copySpec.into(versionedCacheDir);
            });
        } else {
            // ideally this would be a CopyTask, but through Gradle 4.9 CopyTask fails to correctly extract symlinks
            project.exec(spec -> {
                spec.executable("tar");
                spec.args("-xzf", inputArchiveFile.getAbsolutePath());
                spec.args("-C", versionedCacheDir.resolve(graalDirectoryName.get()));
                spec.args("--strip-components=1");
            });
        }

        File nativeImageExecutable = getExecutable("native-image");
        if (!nativeImageExecutable.isFile()) {
            project.exec(spec -> {
                File graalUpdateExecutable = getExecutable("gu");
                if (!graalUpdateExecutable.isFile()) {
                    throw new IllegalStateException("Failed to find Graal update binary: " + graalUpdateExecutable);
                }
                spec.executable(graalUpdateExecutable.getAbsolutePath());
                spec.args("install", "native-image");
            });
        }
    }

    // has some overlap with BaseGraalCompileTask#getArchitectureSpecifiedBinaryPath()
    private File getExecutable(String binaryName) {
        return cacheDir.get()
                .resolve(Paths.get(graalVersion.get(), graalDirectoryName.get()))
                .resolve(getArchitectureSpecifiedBinaryPath(binaryName))
                .toFile();
    }

    private Path getArchitectureSpecifiedBinaryPath(String binaryName) {
        switch (Platform.operatingSystem()) {
            case MAC:
                return Paths.get("Contents", "Home", "bin", binaryName);
            case LINUX:
            default:
                throw new IllegalStateException("No GraalVM support for " + Platform.operatingSystem());
        }
    }

    @InputFile
    public final Provider<RegularFile> getInputArchive() {
        return inputArchive;
    }

    public final void setInputArchive(Provider<RegularFile> value) {
        this.inputArchive.set(value);
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

    final void setCacheDir(Path value) {
        cacheDir.set(value);
    }

    final void setGraalDirectoryName(Provider<String> value) {
        graalDirectoryName.set(value);
    }
}
