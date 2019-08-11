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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import org.gradle.process.ExecSpec;


public abstract class BaseGraalCompileTask extends DefaultTask {
    private final Property<String> outputName = getProject().getObjects().property(String.class);
    private final ListProperty<String> options = getProject().getObjects().listProperty(String.class);
    private final RegularFileProperty outputFile = getProject().getObjects().fileProperty();
    private final Property<String> graalVersion = getProject().getObjects().property(String.class);
    private final Property<Configuration> classpath = getProject().getObjects().property(Configuration.class);
    private final RegularFileProperty jarFile = getProject().getObjects().fileProperty();
    private final Property<Path> cacheDir = getProject().getObjects().property(Path.class);

    public BaseGraalCompileTask() {
        setGroup(GradleGraalPlugin.TASK_GROUP);
        this.outputFile.set(getProject().getLayout().getBuildDirectory()
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
                .resolve(Paths.get(graalVersion.get(), "graalvm-ce-" + graalVersion.get()))
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
            case MAC: return Paths.get("Contents", "Home", "bin", "native-image");
            case LINUX: return Paths.get("bin", "native-image");
            case WINDOWS: return Paths.get("bin", "native-image.cmd");
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
            // all this does though, is setting a bunch of environment variables
            // the main ones are set here to avoid the need for running inside the Windows SDK Command Prompt
            // reference (with installed SDK): C:\Program Files\Microsoft SDKs\Windows\v7.1\Bin\SetEnv.cmd
            // assuming x64 as current and target cpu

            String programFilesx86 = System.getenv("ProgramFiles(x86)");
            String programFiles = System.getenv("ProgramFiles");
            String winDir = System.getenv("WinDir");
            String path = System.getenv("Path");

            String regKeyPath = "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\VisualStudio\\SxS\\VC7";
            String frameworkDir32 = winDir + "\\Microsoft.NET\\Framework\\";
            String frameworkDir64 = readWindowsRegistryString(regKeyPath, "FrameworkDir64");
            String frameworkVer32 = readWindowsRegistryString(regKeyPath, "FrameworkVer32");
            String frameworkVer64 = readWindowsRegistryString(regKeyPath, "FrameworkVer64");

            String frameworkDir = frameworkDir32;
            String frameworkVersion = frameworkVer32;

            if (frameworkDir64 != null && new File(frameworkDir64).exists()) {
                frameworkDir = frameworkDir64;
                frameworkVersion = frameworkVer64;
            }

            regKeyPath = "HKEY_LOCAL_MACHINE\\SOFTWARE\\Wow6432Node\\Microsoft\\VisualStudio\\SxS\\VC7";
            String vcInstallDir = readWindowsRegistryString(regKeyPath, "10.0");
            if (vcInstallDir == null) {
                vcInstallDir = programFilesx86 + "\\Microsoft Visual Studio 10.0\\VC\\";
            }

            String vsRegKeyPath = "HKEY_LOCAL_MACHINE\\SOFTWARE\\Wow6432Node\\Microsoft\\VisualStudio\\SxS\\VS7";
            String vsInstallDir = readWindowsRegistryString(vsRegKeyPath, "10.0");
            if (vsInstallDir == null) {
                vsInstallDir = programFilesx86 + "\\Microsoft Visual Studio 10.0\\";
            }

            String envCl = "/AI " + frameworkDir + "\\" + frameworkVersion;
            getLogger().debug("[env] CL: {}", envCl);
            spec.environment("CL", envCl);

            String vcTools = vcInstallDir + "Bin";

            String winSdkRegKeyPath = "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Microsoft SDKs\\Windows\\v7.1";
            String windowsSdkDir = readWindowsRegistryString(winSdkRegKeyPath, "InstallationFolder");
            if (windowsSdkDir == null) {
                windowsSdkDir = programFiles + "%ProgramFiles%\\Microsoft SDKs\\Windows\\v7.1\\";
            }

            // current_cpu: x64, target_cpu: x64
            vcTools = vcTools + "\\amd64;" + vcTools + "\\VCPackages;";
            String sdkTools = windowsSdkDir + "Bin\\NETFX 4.0 Tools\\x64;"
                    + windowsSdkDir + "Bin\\x64;"
                    + windowsSdkDir + "Bin;";
            String fxTools = frameworkDir64 + "\\" + frameworkVersion + ";"
                    + frameworkDir32 + frameworkVersion + ";"
                    + winDir + "\\Microsoft.NET\\Framework64\\v3.5;"
                    + winDir + "\\Microsoft.NET\\Framework\\v3.5;";

            String vsTools = vsInstallDir + "Common7\\IDE;" + vsInstallDir + "Common7\\Tools;";

            String envPath = fxTools + ";" + vsTools + ";" + vcTools + ";" + sdkTools + ";" + path;
            getLogger().debug("[env] PATH: {}", envPath);
            spec.environment("PATH", envPath);

            String vcLibraries = vcInstallDir + "Lib";
            String vcIncludes = vcInstallDir + "INCLUDE";
            String osLibraries = windowsSdkDir + "Lib";
            String osIncludes = windowsSdkDir + "INCLUDE;" + windowsSdkDir + "INCLUDE\\gl";

            String envLib = vcLibraries + "\\amd64;" + osLibraries + "\\X64";
            String envLibPath = fxTools + ";" + vcLibraries + "\\amd64";
            String envInclude = vcIncludes + ";" + osIncludes;

            if (new File(vcInstallDir, "ATLMFC").exists()) {
                envInclude += ";" + vcInstallDir + "ATLMFC\\INCLUDE";
                envLib += ";" + vcInstallDir + "ATLMFC\\LIB";
            }

            getLogger().debug("[env] LIB: {}", envLib);
            spec.environment("LIB", envLib);

            getLogger().debug("[env] LIBPATH: {}", envLibPath);
            spec.environment("LIBPATH", envLibPath);

            getLogger().debug("[env] INCLUDE: {}", envInclude);
            spec.environment("INCLUDE", envInclude);

            String envAppVer = "6.1";
            getLogger().debug("[env] APPVER: {}", envAppVer);
            spec.environment("APPVER", envAppVer);
        }
    }

    /*
     This regex basically means:
     - from the start (^), find the first occurrence of "REG_" after some other stuff (.+?, + = one or more, ? = lazy)
     - keep going as long it's not whitespace (\S)
     - hop over all the whitespace (\s)
     - read the rest of the line until the end ($) into group 1 (the parens)

     Note that this fails, if the name contains "REG_", but it's ok here, since such keys aren't queried.
     */
    private static final Pattern regValueLine = Pattern.compile("^.+?REG_\\S+\\s+(.*)$");

    private String readWindowsRegistryString(String key, String name) {
        // launch: reg query <key> /v <name>
        ProcessBuilder procBuilder = new ProcessBuilder(
                "C:\\Windows\\System32\\reg.exe", "query", key, "/v", name);
        Process proc;
        try {
            proc = procBuilder.start();
        } catch (IOException e) {
            throw new RuntimeException("Error querying windows registry: couldn't launch reg.exe", e);
        }

        /* example output:
        cmd> reg query HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\VisualStudio\SxS\VC7 /v FrameworkDir64

        HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\VisualStudio\SxS\VC7
            FrameworkDir64    REG_SZ    C:\WINDOWS\Microsoft.NET\Framework64

         */
        BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));

        // find the first line that contains some non-whitespace and starts with whitespace
        Optional<String> firstValueLine = reader.lines()
                .filter(line -> line.trim().length() > 0 && line.matches("^\\s+.+$"))
                .findFirst();

        // wait for the process to finish
        try {
            proc.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException("Error querying windows registry: thread was interrupted.", e);
        }

        try {
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException("Error querying windows registry: stream couldn't be closed.", e);
        }

        int exitValue = proc.exitValue();
        if (exitValue == 1 || !firstValueLine.isPresent()) {
            // key or name was not found
            getLogger().debug("[registry] {} -> {} = not found (exit code: {})", key, name, exitValue);
            return null;
        }

        String line = firstValueLine.get();
        Matcher matcher = regValueLine.matcher(line);
        if (!matcher.find()) {
            getLogger().error("[registry] unexpected line format:\n{}", line);
            throw new RuntimeException("Error querying windows registry: unexpected line format.");
        }

        String value = matcher.group(1);
        getLogger().debug("[registry] {} -> {} = {}", key, name, value);
        return value;
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
