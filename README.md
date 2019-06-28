gradle-graal
------------
A simple wrapper around GraalVM tooling that will download and locally cache a GraalVM installation and make
available select parts of the GraalVM compiler for use in Gradle builds.

To use this plugin, apply `com.palantir.graal`. See a full example in the
[ETE tests](src/test/groovy/com/palantir/gradle/graal/GradleGraalEndToEndSpec.groovy).

Gradle Tasks
------------
`./gradlew nativeImage`: create a native image using GraalVM's `native-image` tool with the configuration as specified
by the `graal` Gradle extension. Outputs are produced to `${projectDir}/build/graal/`.

`./gradlew sharedLibary`: create a shared library using GraalVM's `native-image` tool with the configuration as specified
by the `graal` Gradle extension. Outputs are produced to `${projectDir}/build/graal/`.

Configuration
-------------
Configure this plugin and its wrappers around GraalVM tools through the `graal` extension with the following options:

**General GraalVM controls**
* `graalVersion`: the version string to use when downloading GraalVM (defaults to `19.0.2`)
* `downloadBaseUrl`: the base download URL to use (defaults to `https://github.com/oracle/graal/releases/download/`)


**`native-image` controls**
* `outputName`: the name to use for the image output
* `mainClass`: the main class entry-point for the image to run
* `option`: additional native-image options `https://github.com/oracle/graal/blob/master/substratevm/OPTIONS.md`

Local GraalVM Tooling Cache
---------------------------
We maintain a number of different repositories, and rather than re-download tooling and cache it per repository, this
plugin maintains a central cache in the user's home directory (`~/.gradle/caches/com.palantir.graal`). Tooling artifacts 
are cached by version, so multiple projects referring to different GraalVM versions will not corrupt the cache.

No locking is performed to check the atomicity of changes to the cache, so users should not expect this plugin to be
well behaved when populating the cache from parallel processes.

Contributions
-------------
Contributions are welcome. For larger feature requests or contributions, we prefer discussing the proposed change on 
a GitHub issue prior to a PR.

License
-------
This plugin is made available under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0).
