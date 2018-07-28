# Samples for the Kotlin New MPP

The new multiplatform projects all use a single plugin, `kotlin-multiplatform` (a.k.a. `org.jetbrains.kotlin.multiplatform`) and are then
configured using the special DSL.

## Design

The key concepts in the new MPP model are the following:

* a *Kotlin source set* is a set of Kotlin sources along with their dependencies, resources and language settings;

  * a *Kotlin source set* by itself is platform-agnostic (it can, however, be considered platform-specific when it is only 
    compiled for a single platform);

  * during an import into an IDE, a *Kotlin source set* becomes an *IDE module*;
  
  * (TBD) the language settings of a *Kotlin source set* define the language and API versions, language features etc.
  
  * (TBD) *Kotlin source sets* may be connected with the *depends on* relation, which 
    
    * sets up declaration visibility,
    * allows providing `actual` declarations for the `expect` ones in another *Kotlin source set*,
    * imposes restriction on the language settings of interconnected *Kotlin source set*,
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
  
  * Currently, there are several presets in the `kotlin-gradle-plugin` module, and more will be added by the Kotlin Native Gradle plugin:
  
    * `jvm` is a basic preset for Kotlin/JVM. *NB:* it does not compile Java nor apply the Gradle `java` plugin;
    * `jvmWithJava` is a preset for JVM that is coupled with the Gradle `java` plugin and defines a *Kotlin compilation* per Java source set;
    * `js` is a basic preset for Kotlin/JS;
    * `android` is a preset for Android applications and libraries, it requires one of the Android Gradle plugins to be applied and therefore
      conflicts with the `jvmWithJava` preset;
  
## Gradle plugin and DSL

> Gradle Kotlin DSL is not yet supported, use the Groovy DSL for now

To set up a multiplatform project, one needs to first apply the Kotlin multiplatform plugin:

```groovy
buildscript {
    repositories {
        jcenter()
        maven { url 'https://dl.bintray.com/kotlin/kotlin-dev' }
    }
    dependencies {
        classpath 'org.jetbrains.kotlin:kotlin-gradle-plguin:1.2.70-dev-757` // custom build with new MPP
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
                api 'org.jetbrains.kotlin:kotlin-stdlib:common'
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

* Two source sets, `commonMain` and `commonTest`, are by default created and added to the compilations of production and test sources,
  respectively, of all targets.
  
  * (TBD) Default production source sets of all targets depend on `commonMain`; default test source sets of all targets depend on `commonTest`.

* For a compilation `foo` of a target `bar`, a source set `barFoo` is automatically created and linked to the compilation;
  * The preset `jvmWithJava` automatically creates and links a Kotlin compilation and a Kotlin source set of the same name to a 
    Java source set;
  * The preset `android` creates a Kotlin source set for each Android source set.

* Presets `jvm` and `js` automatically define two compilations, `main` and `test` (with `test` compiled against the outputs of `main`);
  * The preset `android` defines a Kotlin compilation per Android variant, and links the Kotlin source set related to the Android source sets
    participating in the Android variant compilation.
    
How to configure source sets beyond defaults:

```groovy
kotlin {
    sourceSets {
        allJvmMain { /* ... */ }
        jvm6Main {
            dependsOn allJvmMain // (TBD -- not supported in the current DSL, see the alternative below)
        }
        jvm8Main {
            dependsOn allJvmMain // (TBD -- not supported in the current DSL, see the alternative below)
        }
    }
}
```

If a custom source set is not used as a dependency for any other source set and should only be included into a Kotlin compilation:

```groovy
kotlin {
    sourceSets {
        allJvmMain { /* ... */ }
        /* ... */
    }
    targets {
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
}
```

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

Now, in another project, you can declare a single dependency on the published MPP library:

```groovy
// in a module 'my-mpp-app'
kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation 'com.example.mpplib:my-mpp-lib:1.0.0'
            }
        }
    }
}
```

Gradle will resolve the dependency on `my-mpp-lib` into compatible variants of each target in `my-mpp-app`. The complete build scripts
can be found in the [`lib-and-app`](https://github.com/h0tk3y/k-new-mpp-samples/tree/master/lib-and-app) sample.

## IDE plugin

A custom build of an IntelliJ IDEA plugin with the new MPP import support can be found [here](https://teamcity.jetbrains.com/viewLog.html?buildId=1541633&buildTypeId=Kotlin_dev_CompilerAllPlugins&tab=artifacts).
Note that it does not correctly analyze `expect`/`actual` declarations because of the source set dependencies not implemented yet.

