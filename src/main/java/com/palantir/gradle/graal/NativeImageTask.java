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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

/** Runs GraalVM's native-image command with configured options and parameters. */
public class NativeImageTask extends DefaultTask {

    private final Property<String> mainClass = getProject().getObjects().property(String.class);
    private final Property<String> outputName = getProject().getObjects().property(String.class);
    private final Property<String> graalVersion = getProject().getObjects().property(String.class);
    private final ListProperty<String> options = getProject().getObjects().listProperty(String.class);
    private final Property<Configuration> classpath = getProject().getObjects().property(Configuration.class);
    private final RegularFileProperty jarFile = newInputFile();
    private final RegularFileProperty outputFile = newOutputFile();
    private final Property<Path> cacheDir = getProject().getObjects().property(Path.class);

    public NativeImageTask() {
        setGroup(GradleGraalPlugin.TASK_GROUP);
        setDescription("Runs GraalVM's native-image command with configured options and parameters.");

        this.outputFile.set(getProject().getLayout().getBuildDirectory()
                .dir("graal")
                .map(d -> d.file(outputName.get())));

        doLast(t -> {
            getLogger().warn("native-image available at {} ({}MB)",
                    getProject().relativePath(outputFile.get().getAsFile()),
                    fileSizeMegabytes(outputFile.get()));
        });
    }

    @TaskAction
    public final void nativeImage() throws IOException {
        List<String> args = new ArrayList<>();
        args.add("-cp");
        args.add(generateClasspathArgument());
        args.add("-H:Path=" + maybeCreateOutputDirectory().getAbsolutePath());
        if (outputName.isPresent()) {
            args.add("-H:Name=" + outputName.get());
        }
        if (options.isPresent()) {
            List<String> optionList = options.get();
            args.addAll(optionList);
        }

        args.add(mainClass.get());

        getProject().exec(spec -> {
            spec.executable(getExecutable());
            spec.args(args);
        });
    }

    private File maybeCreateOutputDirectory() throws IOException {
        File directory = getOutputFile().get().getAsFile().getParentFile();
        Files.createDirectories(directory.toPath());
        return directory;
    }

    private String getExecutable() {
        return cacheDir.get()
                .resolve(Paths.get(graalVersion.get(), "graalvm-ce-" + graalVersion.get()))
                .resolve(getArchitectureSpecifiedBinaryPath())
                .toFile()
                .getAbsolutePath();
    }

    private String generateClasspathArgument() {
        Set<File> classpathArgument = new LinkedHashSet<>();

        classpathArgument.addAll(classpath.get().getFiles());
        classpathArgument.add(jarFile.getAsFile().get());

        return classpathArgument.stream().map(File::getAbsolutePath).collect(Collectors.joining(":"));
    }

    private Path getArchitectureSpecifiedBinaryPath() {
        switch (Platform.operatingSystem()) {
            case MAC: return Paths.get("Contents", "Home", "bin", "native-image");
            case LINUX: return Paths.get("bin", "native-image");
            default:
                throw new IllegalStateException("No GraalVM support for " + Platform.operatingSystem());
        }
    }

    private long fileSizeMegabytes(RegularFile regularFile) {
        try {
            return Files.size(regularFile.getAsFile().toPath()) / (1000 * 1000);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Input
    public final Provider<String> getMainClass() {
        return mainClass;
    }

    public final void setMainClass(Provider<String> provider) {
        mainClass.set(provider);
    }

    @Input
    public final Provider<String> getOutputName() {
        return outputName;
    }

    @Input
    public final Provider<String> getGraalVersion() {
        return graalVersion;
    }

    public final void setGraalVersion(Provider<String> provider) {
        graalVersion.set(provider);
    }

    @InputFiles
    @Classpath
    public final Provider<Configuration> getClasspath() {
        return classpath;
    }

    public final void setClasspath(Provider<Configuration> provider) {
        classpath.set(provider);
    }

    @InputFile
    public final Provider<RegularFile> getJarFiles() {
        return jarFile;
    }

    public final void setJarFile(Provider<File> provider) {
        jarFile.set(getProject().getLayout().file(provider));
    }

    @OutputFile
    public final Provider<RegularFile> getOutputFile() {
        return outputFile;
    }

    public final void setOutputName(Provider<String> provider) {
        outputName.set(provider);
    }

    final void setCacheDir(Path value) {
        cacheDir.set(value);
    }

    public final void setOptions(ListProperty<String> options) {
        this.options.set(options);
    }
}
