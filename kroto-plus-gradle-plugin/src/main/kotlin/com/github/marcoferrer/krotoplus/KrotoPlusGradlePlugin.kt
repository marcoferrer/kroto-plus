package com.github.marcoferrer.krotoplus

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction


class KrotoPlusGradlePlugin : Plugin<Project> {

    override fun apply(project: Project) {

        project.extensions.create(
                "krotoPlus",
                KrotoPlusPluginExtension::class.java,
                project.objects
        )

        project.tasks.create(
                "generateKrotoPlus",
                GenerateKrotoTask::class.java
        )
    }
}

open class GenerateKrotoTask : DefaultTask() {

    @TaskAction
    open fun generate() {

        val krotoPlusExt = this.project.extensions.getByName("krotoPlus") as KrotoPlusPluginExtension

        val cliArgs = mutableListOf(
                //Path to CLI jar
                //This is a hacky work around until I can refactor the plugin to use the compiler classes directly
                //Groovy GStringImpl is causing issues when passing string typed array
                Manifest::class.java.protectionDomain.codeSource.location.toURI().path
        ).also {
            it.addAll(krotoPlusExt.toCliArgs())
        }

        //Execute CLI
        project.javaexec{
            it.main = "-jar"
            it.args = cliArgs
        }
    }
}