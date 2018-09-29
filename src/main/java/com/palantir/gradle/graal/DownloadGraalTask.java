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
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

/** Downloads GraalVM binaries to {@link GradleGraalPlugin#CACHE_DIR}. */
public class DownloadGraalTask extends DefaultTask {

    private static final String ARTIFACT_PATTERN = "[url]/vm-[version]/graalvm-ce-[version]-[os]-[arch].tar.gz";
    private static final String FILENAME_PATTERN = "graalvm-ce-[version]-[arch].tar.gz";

    private final Provider<String> graalVersion;
    private final Provider<String> downloadBaseUrl;

    @Inject
    public DownloadGraalTask(GraalExtension extension) {
        onlyIf(task -> !getTgz().get().exists());
        setGroup(GradleGraalPlugin.TASK_GROUP);
        setDescription("Downloads and caches GraalVM binaries.");

        graalVersion = extension.getGraalVersion();
        downloadBaseUrl = extension.getDownloadBaseUrl();
    }

    @TaskAction
    public final void downloadGraal() throws IOException {
        Path cache = getCache().get();
        Files.createDirectories(cache);
        try (InputStream in = new URL(render(ARTIFACT_PATTERN)).openStream()) {
            Files.copy(in, getTgz().get().toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @OutputFile
    public final Provider<File> getTgz() {
        return getCache().map(cacheDir -> cacheDir.resolve(render(FILENAME_PATTERN)).toFile());
    }

    @Input
    public final Provider<String> getGraalVersion() {
        return graalVersion;
    }

    @Input
    public final Provider<String> getDownloadBaseUrl() {
        return downloadBaseUrl;
    }

    private Provider<Path> getCache() {
        return graalVersion.map(version -> GradleGraalPlugin.CACHE_DIR.resolve(version));
    }

    private String render(String pattern) {
        return pattern
                .replaceAll("\\[url\\]", downloadBaseUrl.get())
                .replaceAll("\\[version\\]", graalVersion.get())
                .replaceAll("\\[os\\]", getOperatingSystem())
                .replaceAll("\\[arch\\]", getArchitecture());
    }

    private String getOperatingSystem() {
        switch (Platform.operatingSystem()) {
            case MAC:
                return "macos";
            case LINUX:
                return "linux";
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
}
