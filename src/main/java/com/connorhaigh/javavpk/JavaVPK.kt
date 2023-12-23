package com.connorhaigh.javavpk

import com.connorhaigh.javavpk.core.Archive
import java.io.File

object JavaVPK {
    private const val INPUT_OPTION: String = "-input"
    private const val OUTPUT_OPTION: String = "-output"
    private const val VERBOSE_OPTION: String = "-verbose"

    /**
     * Main method.
     * @param args application arguments
     */
    @JvmStatic
    fun main(args: Array<String>) {
        // header
        println("JavaVPK")
        println("(C) Connor Haigh 2014")
        println()

        if (args.isEmpty()) {
            // usage
            println("Usage:")
            println("\t$INPUT_OPTION\t\tSpecify the input VPK file")
            println("\t$OUTPUT_OPTION\t\tSpecify the output directory")
            println("\t$VERBOSE_OPTION\tToggle verbose output")

            return
        }

        // parameters
        var input: String? = null
        var output: String? = null
        var verbose = false

        try {
            // loop arguments
            var argument = 0
            while (argument < args.size) {
                when (args[argument]) {
                    INPUT_OPTION -> input = args[++argument]
                    OUTPUT_OPTION -> output = args[++argument]
                    VERBOSE_OPTION -> verbose = true
                }
                argument++
            }

            // check arguments
            if (input == null || output == null) {
                throw Exception()
            }
        } catch (exception: Exception) {
            // invalid arguments
            System.err.println("Invalid arguments specified")

            return
        }

        // create files
        val inputFile = File(input)
        val outputDirectory = File(output)

        try {
            // create directory
            println("Creating output directory...")
            outputDirectory.mkdirs()

            // load
            val archive =
                Archive(inputFile).also {
                    println("Loading archive...")
                    it.load()
                }

            if (verbose) {
                // details
                println("\t${inputFile.name}")
                println("\tSignature: ${archive.signature}")
                println("\tVersion: ${archive.version}")
                println("\tDirectories: ${archive.directories.size}")
            }

            // loop directories
            println("Extracting all entries...")
            for (directory in archive.directories) {
                if (verbose) {
                    println("\t" + directory.path)
                }

                // loop entries
                for (entry in directory.entries) {
                    if (verbose) {
                        println("\t\t" + entry.fullName)
                    }

                    try {
                        // extract
                        val entryDirectory = File(outputDirectory, directory.path)
                        val entryFile = File(outputDirectory, directory.getPathFor(entry))
                        entryDirectory.mkdirs()
                        entry.extract(entryFile)
                    } catch (exception: Exception) {
                        throw exception
                    }
                }
            }

            // done
            println("Extracted all entries successfully")
        } catch (exception: Exception) {
            // failed
            System.err.println()
            System.err.println("Error during extraction: ${exception.message}")
        }
    }
}
