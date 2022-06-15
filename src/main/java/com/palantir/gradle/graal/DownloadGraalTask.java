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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

/** Downloads GraalVM binaries. */
public class DownloadGraalTask extends DefaultTask {

    // RC versions don't have a windows variant, so no [ext] is needed
    private static final String ARTIFACT_PATTERN_RC_VERSION =
            "[url]/vm-[version]/graalvm-ce-[javaVersion]-[version]-[os]-[arch].tar.gz";
    private static final String ARTIFACT_PATTERN_RELEASE_VERSION =
            "[url]/vm-[version]/graalvm-ce-[javaVersion]-[os]-[arch]-[version].[ext]";

    private static final String FILENAME_PATTERN = "graalvm-ce-[javaVersion]-[version]-[arch].[ext]";

    private final Property<String> graalVersion = getProject().getObjects().property(String.class);
    private final Property<String> javaVersion = getProject().getObjects().property(String.class);
    private final Property<String> downloadBaseUrl = getProject().getObjects().property(String.class);
    private final Property<Path> cacheDir = getProject().getObjects().property(Path.class);

    public DownloadGraalTask() {
        setGroup(GradleGraalPlugin.TASK_GROUP);
        setDescription("Downloads and caches GraalVM binaries.");

        onlyIf(_task -> !getArchive().get().getAsFile().exists());
    }

    @TaskAction
    public final void downloadGraal() throws IOException {
        Files.createDirectories(getArchive().get().getAsFile().toPath().getParent());

        final String artifactPattern =
                isGraalRcVersion() ? ARTIFACT_PATTERN_RC_VERSION : ARTIFACT_PATTERN_RELEASE_VERSION;

        try (InputStream in = new URL(render(artifactPattern)).openStream()) {
            Files.copy(in, getArchive().get().getAsFile().toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @OutputFile
    public final Provider<RegularFile> getArchive() {
        return getProject().getLayout().file(getCacheSubdirectory().map(dir -> dir.resolve(javaVersion.get())
                .resolve(render(FILENAME_PATTERN))
                .toFile()));
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
    public final Provider<String> getDownloadBaseUrl() {
        return downloadBaseUrl;
    }

    public final void setDownloadBaseUrl(Provider<String> provider) {
        downloadBaseUrl.set(provider);
    }

    private Provider<Path> getCacheSubdirectory() {
        return cacheDir.map(dir -> dir.resolve(graalVersion.get()));
    }

    private String render(String pattern) {
        final String computedJavaVersion = GraalVersionUtil.isGraalVersionGreaterOrEqualThan(graalVersion.get(), 19, 3)
                ? "java" + javaVersion.get()
                : ""; // for GraalVM >= 19.3 the naming contains java8 or java11
        return pattern.replaceAll("\\[url\\]", downloadBaseUrl.get())
                .replaceAll("\\[version\\]", graalVersion.get())
                .replaceAll("\\[javaVersion\\]", computedJavaVersion)
                .replaceAll("\\[os\\]", getOperatingSystem())
                .replaceAll("\\[arch\\]", getArchitecture())
                .replaceAll("\\[ext\\]", getArchiveExtension())
                .replaceAll("--", "-"); // for GraalVM < 19.3 there's only a Java8 package
    }

    private String getOperatingSystem() {
        switch (Platform.operatingSystem()) {
            case MAC:
                return isGraalRcVersion() ? "macos" : "darwin";
            case LINUX:
                return "linux";
            case WINDOWS:
                return "windows";
            default:
                throw new IllegalStateException("No GraalVM support for " + Platform.operatingSystem());
        }
    }

    private String getArchitecture() {
        switch (Platform.architecture()) {
            case AMD64:
                return "amd64";
            default:
                throw new IllegalStateException("No GraalVM support for " + Platform.architecture());
        }
    }

    private String getArchiveExtension() {
        switch (Platform.operatingSystem()) {
            case MAC:
            case LINUX:
                return "tar.gz";
            case WINDOWS:
                return "zip";
            default:
                throw new IllegalStateException("No GraalVM support for " + Platform.operatingSystem());
        }
    }

    private boolean isGraalRcVersion() {
        return graalVersion.get().startsWith("1.0.0-rc");
    }

    final void setCacheDir(Path value) {
        cacheDir.set(value);
    }
}
