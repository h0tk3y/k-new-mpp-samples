# Kotlin New Multiplatform Projects 

This repository contains general information and samples for the new model of Kotlin Multiplatform projects.

## Goals

1. Make project configuration more concise: allow to define all target platforms in a single Gradle subproject 
2. Enable more granular code sharing between platforms. Previously there was only one common part shared between all platforms (e.g one common part shared between JVM, JS and Native). Now it is possible to share a part of code between some platforms (e.g. there can be one part for JVM, JS and Native and one part shared between JVM and JS).
3. Enable publishing of MPP libraries and simplify dependency management when it comes to multiplatform libraries.
4. Unify Kotlin/Native and non-Native DSLs.

## Design

The key concepts in the new MPP model are the following:

* a *Kotlin source set* is a set of Kotlin sources along with their dependencies, resources and language settings;

  * a *Kotlin source set* by itself is platform-agnostic (it can, however, be considered platform-specific when it is only 
    compiled for a single platform);

  * during an import into an IDE, a *Kotlin source set* becomes an *IDE module*;
  
  * the language settings of a *Kotlin source set* define the language and API versions, language features etc.
  
  * *Kotlin source sets* may be connected with the *depends on* relation, which 
    
    * sets up declaration visibility,
    * allows providing `actual` declarations for the `expect` ones in another *Kotlin source set*,
    * imposes restriction on the language settings of interconnected *Kotlin source sets*,
    * defines dependency relations of the IDE modules corresponding to the source sets;

* a *Kotlin target* is a part of a Gradle project that can be thought of as a build of a complete piece of software targeting a single platform such as JVM 6, Android, JS for a browser, iOS, Linux x64, etc.;

  * a *Kotlin target* is the closest thing to a platform module in the 1.2.x MPP design;
  
  * a *Kotlin target* defines its own specific testing routines, if any, and publications;

* a *Kotlin compilation* is an established transformation of source files (one or more *Kotlin source sets*, plus, potentially, Java sources, 
Android resources) into a binary form, such as Kotlin metadata, JVM class files, JavaScript code, or a klib;

  * a *Kotlin compilation* is set up to compile a collection of *Kotlin source sets*;
  
  * a *Kotlin compilation* may include additional dependencies to those collected from the *Kotlin source sets*;

  * a *Kotlin target* may, and usually does, include more than one compilation: there are separate compilations for main (production) sources 
    and test sources, separate compilations for each Android variant and each Java source set;
    
* a *Kotlin target preset* is a way to create a *Kotlin target*; it is enough to provide just a name to create a *target* from a *preset*, but
  the resulting target may be further configured through its (target-specific) API.
  
  * Currently, there are several presets in the `kotlin-gradle-plugin` module:
  
    * `jvm` is a basic preset for Kotlin/JVM. *NB:* it does not compile Java nor apply the Gradle `java` plugin;
    * `jvmWithJava` is a preset for JVM that is coupled with the Gradle `java` plugin and defines a *Kotlin compilation* per Java source set; this preset is likely to be removed in the future once `jvm` gets support for compiling Java;
    * `js` is a basic preset for Kotlin/JS;
    * `android` is a preset for Android applications and libraries, it requires one of the Android Gradle plugins to be applied and therefore conflicts with the `jvmWithJava` preset;
    * Kotlin/Native presets (see [the notes below](#notes-on-kotlinnative-support)):
        * `androidNativeArm32` and `androidNativeArm64` for Android NDK
        * `iosArm32`, `iosArm64`, `iosX64` for iOS
        * `linuxArm32Hfp`, `linuxMips32`, `linuxMipsel32`, `linuxX64` for Linux
        * `macosX64` for MacOS
        * `mingwX64` for Windows
  
## Gradle plugin and DSL

The new multiplatform projects all use a single plugin, `kotlin-multiplatform` (a.k.a. `org.jetbrains.kotlin.multiplatform`) and are then
configured using the special DSL.

> Gradle Kotlin DSL is not yet supported, use the Groovy DSL for now

To set up a multiplatform project, first apply the Kotlin multiplatform plugin:

```groovy
buildscript {
    repositories {
        jcenter()
        maven { url 'https://dl.bintray.com/kotlin/kotlin-dev' }
    }
    dependencies {
        classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.0-dev-1'
    }
}

repositories {
    jcenter()
    maven { url 'https://dl.bintray.com/kotlin/kotlin-dev' }
}

apply plugin: 'kotlin-multiplatform'
```

Then you can configure *Kotlin targets* and *Kotlin source sets* through the top-level `kotlin { ... }` extension.

Targets basic example:

```groovy
kotlin {
    targets {
        fromPreset(presets.jvm, 'jvm6') // Create a target by the name 'jvm6'
        fromPreset(presets.js, 'nodeJs')
    }
}
```

Source sets basic example:

```groovy
kotlin {
    targets { /* ... */ }
    sourceSets {
        commonMain { // `commonMain` is the default source set added to production compilations of all targets
            dependencies {
                api 'org.jetbrains.kotlin:kotlin-stdlib-common'
            }
        }
        commonTest { // `commonTest` is the default source set included into all test compilations
            dependencies {
                implementation 'org.jetbrains.kotlin:kotlin-test-common'
                implementation 'org.jetbrains.kotlin:kotlin-test-annotations-common'
            }
        }
        jvm6Main { // `jvm6Main` is automatically created for the `main` compilation of the target `jvm6`
            dependencies {
                api 'org.jetbrains.kotlin:kotlin-stdlib'
            }
        }
        nodeJsMain { // automatically created for the `main` compilation of the target `nodeJs`
            dependencies {
                api 'org.jetbrains.kotlin:kotlin-stdlib-js'
            }
        }      
    }
}
```

There is a set of *reasonable defaults* aimed to simplify project configuration. Configuring a project beyond these defaults is explained further.
The defaults are:

* Each *Kotlin source set* has a default Kotlin sources directory `src/<sourceSetName>/kotlin`.

* Two source sets, `commonMain` and `commonTest`, are by default created and added to the default (i.e. not defined by the user) compilations of production and test sources,
  respectively, of all targets.
  
  * Default production source sets of all targets depend on `commonMain`; default test source sets of all targets depend on `commonTest`.

* For a compilation `foo` of a target `bar`, a source set `barFoo` is automatically created and linked to the compilation;
  * The preset `jvmWithJava` automatically creates and links a Kotlin compilation and a Kotlin source set for each Java source set, with the same name to that of the 
    Java source set;
  * The preset `android` creates a Kotlin source set for each Android source set.

* Presets `jvm` and `js` automatically define two compilations, `main` and `test` (with `test` compiled against the outputs of `main`);
  * The preset `android` defines a Kotlin compilation per Android variant, and links the Kotlin source set related to the Android source sets
    participating in the Android variant compilation.
    
How to configure source sets beyond defaults:

```groovy
kotlin.sourceSets {
    allJvmMain { /* ... */ }
    jvm6Main {
        dependsOn allJvmMain
    }    
    jvm8Main {
        dependsOn allJvmMain
    }
}
```

If a custom source set is not used as a dependency for any other source set and should only be included into a Kotlin compilation:

```groovy
kotlin.sourceSets {
    allJvmMain { /* ... */ }
    /* ... */
}

kotlin.targets {
    fromPreset(presets.jvm, 'jvm6') {
        compilations.main {
            source(sourceSets.allJvmMain)
        }
        /* ... */
    }
    fromPreset(presets.jvm, 'jvm8') {
        compilations.main {
            source(sourceSets.allJvmMain)
        }
        /* ... */
    }
}
```

Hint: you can configure multiple named entities in a container using the Gradle's [`configure([...]) { ... }`](https://docs.gradle.org/current/javadoc/org/gradle/api/Project.html#configure-java.lang.Iterable-groovy.lang.Closure-) function:

```groovy
kotlin.targets {
    fromPreset(presets.jvm, 'junit') { /* ... */ }
    fromPreset(presets.jvm, 'testng') { /* ... */ }
    configure([junit, testng]) { 
        tasks.getByName(compilations.main.compileKotlinTaskName).kotlinOptions {
            jvmTarget = '1.8'
        }
    }
}
```



## Language settings

Each source set may specify its language settings with the following DSL, with all of the items being optional:

```groovy
kotlin.sourceSets {
    foo {
        languageSettings {
            languageVersion = '1.2'
            apiVersion = '1.2'
            progressiveMode = true
            enableLanguageFeature('InlineClasses')
        }
    }
}
```

These settings affect the behavior of analysis:

* (TBD) In the IDE, each module created from a source set uses takes its language settings into the facet
* During a Gradle build, the language settings of the default source set created for a compilation are used for the Kotlin compilation task (with the task's own `kotlinOptions` having higher priority on language and API versions)

The language settings are checked for consistency between source sets depending on each other. Namely, if `foo` depends on `bar`:

* `foo` should set `languageVersion` no less than that of `bar`
* `foo` should enable all **unstable** language features that `bar` enabled (but there's no such requirement for bugfix features)
* `apiVersion` and bugfix language features can be set arbitrarily

To ensure consistency between source sets added to a compilation, additional checks are made as if those depended on the compilation's default source set.

## Publishing and MPP library dependencies

To make publishing and dependency resolution work, you should enable the experimental Gradle feature in `settings.gradle`:

```groovy
enableFeaturePreview('GRADLE_METADATA`)
```

Then, to publish an MPP library, use the [`maven-publish` Gradle plugin](https://docs.gradle.org/current/userguide/publishing_maven.html)
and provide proper group and version for the module:

```groovy
// in a module 'my-mpp-lib'
group 'com.example.mpplib'
version '1.0.0'

apply plugin: 'maven-publish`
```

That's it! Publish it to some repository (e.g. to the local Maven repo with the task `publishToMavenLocal`).

Now, in another project, it is enough to declare a single dependency on the published MPP library:

```groovy
// in a module 'my-mpp-app'
repositories {
   // Add the repository where 'my-mpp-lib' is published, is it mavenLocal()?
}

kotlin.sourceSets {
    commonMain {
        dependencies {
            implementation 'com.example.mpplib:my-mpp-lib:1.0.0'
        }
    } 
}
```

Gradle will resolve the dependency on `my-mpp-lib` into a compatible variant for each target in `my-mpp-app`. The complete build scripts
can be found in the [`lib-and-app`](https://github.com/h0tk3y/k-new-mpp-samples/tree/master/lib-and-app) sample.

Even a platform-specific Kotlin source set may depend on a MPP library, which results in the library's compatible variant resolved for the compilation.

A project dependency on another MPP project has the same semantics and is also resolved in a variant-aware way:

```
dependencies {
    implementation project(':my-mpp-lib')
}
```

## Metadata publishing for common sources

To provide tooling support for libraries code, we build and publish so-called *Kotlin metadata* containing serialized declarations from the sources. The metadata artifacts are used by the IDE for analysis, and by the Gradle plugin for producing metadata of other projects.

The Kotlin metadata artifact of a project is published along with the other artifacts, but in a separate variant, meaning that it can be resolved in a configuration with matching attributes (`org.jetbrains.kotlin.platform.type = common`).

Each Kotlin source set has several Gradle configurations which can be resolved to retrieve the metadata of its dependencies, such as `apiDependenciesMetadata` for `api` dependencies, `implementationDependenciesMetadata` for `implementation` etc.

A Kotlin target by the name *metadata* is responsible for building and publishing the metadata artifact.

We are going to implement this in two steps.

* The initial implementation only produces Kotlin metadata for sources in the `commonMain` source set. This limits tooling support for non-default source sets in common sources of dependent projects, more specifically, declarations from source sets other than `commonMain` and the platform specific source sets are not going to be analyzed correctly at first.

* (TBD) Afterwards, we will improve the mechanism by producing metadata for all source sets in a project and determining which source sets from a library are relevant during the IDE import.

## Notes on Kotlin/Native support

* Some targets [may only be built with an appropriate host](https://github.com/JetBrains/kotlin/blob/2251440f04e5bdb4bdfef2dc47c30356b7f39411/konan/utils/src/org/jetbrains/kotlin/konan/target/KonanTarget.kt#L168) (e.g. a Windows machine cannot build Linux or iOS native artifacts). An unsupported target is skipped during builds.

    * (TBD) During publishing with the `maven-publish` plugin, only artifacts for targets supported by the host should be published. Currently, publishing from a host that does not support some of the targets erases their artifacts from the Gradle metadata.

* To build an executable for a Kotlin/Native target, say, a `linuxX64` one, add the following to the build script:

    ```groovy
    import org.jetbrains.kotlin.gradle.plugin.mpp.NativeOutputKind
    
    /* ... */
    
    kotlin.targets {
        fromPreset(presets.linuxX64, 'foo') {
            compilations.main.outputKinds += NativeOutputKind.EXECUTABLE
        }
    }
    ```

## IDE plugin

Download the IDE plugin build from TeamCity: ([link](https://teamcity.jetbrains.com/viewLog.html?buildId=lastSuccessful&buildTypeId=Kotlin_dev_CompilerAllPlugins&tab=artifacts), log in as guest if you have no account). Then install the plugin from disk in IntelliJ IDEA.

## Known issues

Use this search query in the Kotlin YouTrack: [(link)](https://youtrack.jetbrains.com/issues/KT?q=%23new-multiplatform%20)
