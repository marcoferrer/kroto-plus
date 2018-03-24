package com.github.marcoferrer.krotoplus.cli

import kotlinx.cli.CommandLineInterface
import kotlinx.cli.HelpEntry
import kotlinx.cli.HelpPrinter

fun CommandLineInterface.appendHelpEntry(cli: CommandLineInterface){
    addHelpEntry(object : HelpEntry {
        override fun printHelp(helpPrinter: HelpPrinter) {
            cli.printHelp(helpPrinter)
            helpPrinter.printSeparator()
        }
    })
}