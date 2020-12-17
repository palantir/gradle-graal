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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.process.ExecSpec;

public abstract class BaseGraalCompileTask extends DefaultTask {
    private final Property<String> outputName = getProject().getObjects().property(String.class);
    private final ListProperty<String> options = getProject().getObjects().listProperty(String.class);
    private final RegularFileProperty outputFile = getProject().getObjects().fileProperty();
    private final Property<String> graalVersion = getProject().getObjects().property(String.class);
    private final Property<String> javaVersion = getProject().getObjects().property(String.class);
    private final Property<String> windowsVsVarsPath = getProject().getObjects().property(String.class);
    private final Property<Configuration> classpath = getProject().getObjects().property(Configuration.class);
    private final RegularFileProperty jarFile = getProject().getObjects().fileProperty();
    private final Property<Path> cacheDir = getProject().getObjects().property(Path.class);
    private final Property<String> graalDirectoryName =
            getProject().getObjects().property(String.class);

    protected BaseGraalCompileTask() {
        setGroup(GradleGraalPlugin.TASK_GROUP);
        this.outputFile.set(getProject()
                .getLayout()
                .getBuildDirectory()
                .dir("graal")
                .map(d -> d.file(outputName.get() + getArchitectureSpecifiedOutputExtension())));
    }

    protected abstract String getArchitectureSpecifiedOutputExtension();

    protected final File maybeCreateOutputDirectory() throws IOException {
        File directory = getOutputFile().get().getAsFile().getParentFile();
        Files.createDirectories(directory.toPath());
        return directory;
    }

    protected final String getExecutable() {
        return cacheDir.get()
                .resolve(Paths.get(graalVersion.get(), javaVersion.get(), graalDirectoryName.get()))
                .resolve(getArchitectureSpecifiedBinaryPath())
                .toFile()
                .getAbsolutePath();
    }

    /**
     * Adds all graal vm command line args into the specified args list.
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

        return classpathArgument.stream()
                .map(File::getAbsolutePath)
                .collect(Collectors.joining(getArchitectureSpecifiedPathSeparator()));
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

    private String getArchitectureSpecifiedPathSeparator() {
        switch (Platform.operatingSystem()) {
            case MAC:
            case LINUX:
                return ":";
            case WINDOWS:
                return ";";
            default:
                throw new IllegalStateException("No GraalVM support for " + Platform.operatingSystem());
        }
    }

    protected final void configurePlatformSpecifics(ExecSpec spec) {
        if (Platform.operatingSystem() == Platform.OperatingSystem.WINDOWS) {
            // on Windows the native-image executable needs to be launched from the Windows SDK Command Prompt
            // this is mentioned at https://github.com/oracle/graal/tree/master/substratevm#quick-start
            // here we create and launch a temporary .cmd file that first calls SetEnv.cmd and then runs Graal

            String outputRedirection = "";
            if (!getLogger().isEnabled(LogLevel.INFO)) {
                // hide the output of SetEnv.cmd (an error that can safely be ignored and info messages)
                // if Gradle isn't run with e.g. --info
                outputRedirection = " >nul 2>&1";
            }

            if (windowsVsVarsPath.get().isEmpty()) {
                throw new GradleException("Couldn't find an installation of Windows SDK 7.1 suitable for GraalVM.");
            }

            String argsString =
                    spec.getArgs().stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(" ", " ", "\r\n"));
            String command = "call \"" + windowsVsVarsPath.get() + "\"";
            String cmdContent = "@echo off\r\n"
                    + command
                    + outputRedirection + "\r\n"
                    + "\"" + spec.getExecutable() + "\"" + argsString;
            Path buildPath = getProject().getBuildDir().toPath();
            Path startCmd =
                    buildPath.resolve("tmp").resolve("com.palantir.graal").resolve("native-image.cmd");
            try {
                if (!Files.exists(startCmd.getParent())) {
                    Files.createDirectories(startCmd.getParent());
                }
                Files.write(startCmd, cmdContent.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            List<String> cmdArgs = new ArrayList<>();
            // command extensions
            cmdArgs.add("/E:ON");
            // delayed environment variable expansion via !
            cmdArgs.add("/V:ON");
            cmdArgs.add("/c");
            cmdArgs.add("\"" + startCmd.toString() + "\"");
            spec.setExecutable("cmd.exe");
            spec.setArgs(cmdArgs);
        }
    }

    protected static long fileSizeMegabytes(RegularFile regularFile) {
        try {
            return Files.size(regularFile.getAsFile().toPath()) / (1000 * 1000);
        } catch (IOException e) {
            throw new RuntimeException(e);
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

    @Input
    public final Provider<String> getJavaVersion() {
        return javaVersion;
    }

    public final void setJavaVersion(Provider<String> provider) {
        javaVersion.set(provider);
    }

    @Input
    public final Provider<String> getWindowsVsVarsPath() {
        return windowsVsVarsPath;
    }

    public final void setWindowsVsVarsPath(Provider<String> provider) {
        windowsVsVarsPath.set(provider);
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

    final void setGraalDirectoryName(Provider<String> value) {
        graalDirectoryName.set(value);
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
