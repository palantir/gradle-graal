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

import java.util.List;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

/** Contains options and settings for tuning GraalVM use. */
public class GraalExtension {

    private static final String DEFAULT_DOWNLOAD_BASE_URL = "https://github.com/oracle/graal/releases/download/";
    private static final String DEFAULT_GRAAL_VERSION = "1.0.0-rc12";

    private final Property<String> downloadBaseUrl;
    private final Property<String> graalVersion;
    private final Property<String> mainClass;
    private final Property<String> outputName;
    private final ListProperty<String> options;

    public GraalExtension(Project project) {
        downloadBaseUrl = project.getObjects().property(String.class);
        graalVersion = project.getObjects().property(String.class);
        mainClass = project.getObjects().property(String.class);
        outputName = project.getObjects().property(String.class);
        options = project.getObjects().listProperty(String.class).empty(); // .empty() required to initialize

        // defaults
        downloadBaseUrl.set(DEFAULT_DOWNLOAD_BASE_URL);
        graalVersion.set(DEFAULT_GRAAL_VERSION);
    }

    public final void downloadBaseUrl(String value) {
        downloadBaseUrl.set(value);
    }

    /**
     * Returns the base URL to use for downloading GraalVM binaries.
     *
     * <p>Defaults to {@link #DEFAULT_DOWNLOAD_BASE_URL}.</p>
     */
    public final Provider<String> getDownloadBaseUrl() {
        return downloadBaseUrl;
    }

    public final void mainClass(String value) {
        mainClass.set(value);
    }

    /** Returns the main class to use as the entry point to the generated executable file. */
    public final Provider<String> getMainClass() {
        return mainClass;
    }

    public final void outputName(String value) {
        outputName.set(value);
    }

    /**
     * Returns the outputName to use for the generated executable file.
     *
     * <p>Check {@link org.gradle.api.provider.Provider#isPresent()} to determine if an override has been set.</p>
     */
    public final Provider<String> getOutputName() {
        return outputName;
    }

    public final void graalVersion(String value) {
        graalVersion.set(value);
    }

    /**
     * Returns the graalVersion of GraalVM to use.
     *
     * <p>Defaults to {@link #DEFAULT_GRAAL_VERSION}</p>
     */
    public final Provider<String> getGraalVersion() {
        return graalVersion;
    }


    public final Provider<List<String>> getOptions() {
        return this.options;
    }

    /**
     * Add option from https://github.com/oracle/graal/blob/master/substratevm/OPTIONS.md.
     */
    public final void option(String option) {
        if (option.trim().startsWith("-H:Name=")) {
            throw new GradleException("Use 'outputName' instead of '" + option + "'");
        }
        this.options.add(option);
    }

}
