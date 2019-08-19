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


public class BaseGraalCompileTask extends DefaultTask {
    private final Property<String> outputName = getProject().getObjects().property(String.class);
    private final ListProperty<String> options = getProject().getObjects().listProperty(String.class);
    private final Property<String> graalVersion = getProject().getObjects().property(String.class);
    private final Property<Configuration> classpath = getProject().getObjects().property(Configuration.class);
    private final RegularFileProperty jarFile = getProject().getObjects().fileProperty();
    private final Property<Path> cacheDir = getProject().getObjects().property(Path.class);

    public BaseGraalCompileTask() {
        setGroup(GradleGraalPlugin.TASK_GROUP);
    }

    protected final File maybeCreateOutputDirectory() throws IOException {
        File directory = getOutputFile().get().getAsFile().getParentFile();
        Files.createDirectories(directory.toPath());
        return directory;
    }

    protected final String getExecutable() {
        return cacheDir.get()
                .resolve(Paths.get(graalVersion.get(), "graalvm-ce-" + graalVersion.get()))
                .resolve(getArchitectureSpecifiedBinaryPath())
                .toFile()
                .getAbsolutePath();
    }

    /**
     * Adds all graal vm command line args into the specified args list.
     *
     * @param args The list where all the command line args are going to be loaded
     * @throws IOException If any problem while creating output directory
     */
    protected final void configureArgs(List<String> args) throws IOException {
        args.add("-cp");
        args.add(generateClasspathArgument());
        args.add("-H:Path=" + maybeCreateOutputDirectory().getAbsolutePath());
        if (options.isPresent()) {
            List<String> optionList = options.get();
            args.addAll(optionList);
        }
        // Set H:Name after all other options in order to override other H:Name
        // options that were expanded from macro options above. See
        // https://github.com/oracle/graal/issues/1032
        if (outputName.isPresent()) {
            args.add("-H:Name=" + outputName.get());
        }
    }

    protected final String generateClasspathArgument() {
        Set<File> classpathArgument = new LinkedHashSet<>();

        classpathArgument.addAll(classpath.get().getFiles());
        classpathArgument.add(jarFile.getAsFile().get());

        return classpathArgument.stream().map(File::getAbsolutePath).collect(Collectors.joining(":"));
    }

    private Path getArchitectureSpecifiedBinaryPath() {
        switch (Platform.operatingSystem()) {
            case MAC:
                return Paths.get("Contents", "Home", "bin", "native-image");
            case LINUX:
                return Paths.get("bin", "native-image");
            case WINDOWS:
                return Paths.get("bin", "native-image.cmd");
            default:
                throw new IllegalStateException("No GraalVM support for " + Platform.operatingSystem());
        }
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
        String name = outputName.getOrElse("unknown");
        if (Platform.operatingSystem().equals(Platform.OperatingSystem.WINDOWS)) {
            if (!name.endsWith(".exe")) {
                name = name + ".exe";
            }
        }
        final String finalName = name;
        RegularFileProperty outputFile = getProject().getObjects().fileProperty();
        outputFile.set(getProject().getLayout().getBuildDirectory()
                .dir("graal")
                .map(d -> d.file(finalName)));
        return outputFile;
    }

    public final void setOutputName(Provider<String> provider) {
        outputName.set(provider);
    }

    final void setCacheDir(Path value) {
        cacheDir.set(value);
    }

    public final void setOptions(Provider<List<String>> options) {
        this.options.set(options);
    }

    public final ListProperty<String> getOptions() {
        return options;
    }

    public final RegularFileProperty getJarFile() {
        return jarFile;
    }

    public final Property<Path> getCacheDir() {
        return cacheDir;
    }
}
