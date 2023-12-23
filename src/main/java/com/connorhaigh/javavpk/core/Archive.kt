package com.connorhaigh.javavpk.core

import com.connorhaigh.javavpk.exceptions.ArchiveException
import com.connorhaigh.javavpk.exceptions.EntryException
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Returns the VPK archive file for this archive.
 * @return the VPK archive file
 */
class Archive(val file: File) {
    /**
     * Returns if this archive is made of multiple children (separate VPK archives).
     * @return if this archive is made of multiple children
     */
    var isMultiPart: Boolean = false
        private set

    /**
     * Returns the signature of this archive.
     * In most cases, this should be 0x55AA1234.
     * @return the signature
     */
    var signature: Int = 0
        private set

    /**
     * Returns the internal version of this archive.
     * In most cases, this should be 2.
     * @return the internal version
     */
    var version: Int = 0
        private set

    /**
     * Returns the length of the root tree for this archive.
     * @return the length of the root tree
     */
    var treeLength: Int = 0
        private set

    /**
     * Returns the length of the header for this archive.
     * @return the length of the header
     */
    var headerLength: Int = 0
        private set

    /**
     * Returns the list of directories in this archive.
     * @return the list of directories
     */
    val directories: ArrayList<Directory> = ArrayList()

    /**
     * Load the raw data from file to this archive.
     * @throws IOException if the archive could not be read
     * @throws ArchiveException if a general archive exception occurs
     * @throws EntryException if a general entry exception occurs
     */
    @Throws(IOException::class, ArchiveException::class, EntryException::class)
    fun load() {
        FileInputStream(file).use { fileInputStream ->
            // check for multiple child archives
            isMultiPart = file.name.contains("_dir")

            // read header
            signature = readUnsignedInt(fileInputStream)
            version = readUnsignedInt(fileInputStream)
            treeLength = readUnsignedInt(fileInputStream)

            // check signature and version
            if (signature != SIGNATURE) throw ArchiveException("Invalid signature")
            if (version < MINIMUM_VERSION || version > MAXIMUM_VERSION) throw ArchiveException("Unsupported version")

            when (version) {
                VERSION_ONE -> {
                    headerLength = VERSION_ONE_HEADER_SIZE
                }

                VERSION_TWO -> {
                    headerLength = VERSION_TWO_HEADER_SIZE

                    // read extra data
                    // serves no purpose right now
                    readUnsignedInt(fileInputStream)
                    readUnsignedInt(fileInputStream)
                    readUnsignedInt(fileInputStream)
                    readUnsignedInt(fileInputStream)
                }
            }
            while (fileInputStream.available() != 0) {
                // get extension
                val extension = readString(fileInputStream)
                if (extension.isEmpty()) break

                while (true) {
                    // get path
                    val path = readString(fileInputStream)
                    if (path.isEmpty()) break

                    // directory
                    val directory = Directory(path)
                    directories.add(directory)

                    while (true) {
                        // get filename
                        val filename = readString(fileInputStream)
                        if (filename.isEmpty()) break

                        // read data
                        val crc = readUnsignedInt(fileInputStream)
                        val preloadSize = readUnsignedShort(fileInputStream)
                        val archiveIndex = readUnsignedShort(fileInputStream)
                        val entryOffset = readUnsignedInt(fileInputStream)
                        val entryLength = readUnsignedInt(fileInputStream)
                        val terminator = readUnsignedShort(fileInputStream)
                        var preloadData: ByteArray? = null

                        if (preloadSize > 0) {
                            // read preload data
                            preloadData = ByteArray(preloadSize.toInt())
                            fileInputStream.read(preloadData)
                        }

                        // create entry
                        val entry =
                            Entry(
                                this,
                                archiveIndex,
                                preloadData,
                                filename,
                                extension,
                                crc,
                                entryOffset,
                                entryLength,
                                terminator,
                            )

                        directory.addEntry(entry)
                    }
                }
            }
        }
    }

    /**
     * Returns a child archive that belongs to this parent.
     * @param index the index of the archive
     * @return the child archive, or null
     * @throws ArchiveException if this archive is not made up of multiple children
     */
    @Throws(ArchiveException::class)
    fun getChildArchive(index: Int): File {
        // check
        if (!isMultiPart) {
            throw ArchiveException("Archive is not multi-part")
        }

        // get parent
        val parent = file.parentFile ?: throw ArchiveException("Archive has no parent")

        // get child name
        val fileName = file.name
        val rootName = fileName.substring(0, fileName.length - 8)
        val childName = String.format("%s_%03d.vpk", rootName, index)

        return File(parent, childName)
    }

    /**
     * Reads a stream character by character until a null terminator is reached.
     * @param fileInputStream the stream to read
     * @return the assembled string
     * @throws IOException if the stream could not be read
     */
    @Throws(IOException::class)
    private fun readString(fileInputStream: FileInputStream): String {
        // builder
        val stringBuilder = StringBuilder()

        // read
        var character: Int
        while ((fileInputStream.read().also { character = it }) != NULL_TERMINATOR.code) {
            stringBuilder.append(character.toChar())
        }

        return stringBuilder.toString()
    }

    /**
     * Reads an unsigned integer (4 bytes) from a stream.
     * @param fileInputStream the stream to read
     * @return the unsigned integer
     * @throws IOException if the stream could not be read
     */
    @Throws(IOException::class)
    private fun readUnsignedInt(fileInputStream: FileInputStream): Int = readBytes(fileInputStream, 4).getInt()

    /**
     * Reads an unsigned short (2 bytes) from a stream.
     * @param fileInputStream the stream to read
     * @return the unsigned short
     * @throws IOException if the stream could not be read
     */
    @Throws(IOException::class)
    private fun readUnsignedShort(fileInputStream: FileInputStream): Short = readBytes(fileInputStream, 2).getShort()

    /**
     * Reads the specified amount of bytes from a stream.
     * @param fileInputStream the stream to read
     * @param size the amount of bytes to read
     * @return the byte buffer
     * @throws IOException if the stream could not be read
     */
    @Throws(IOException::class)
    private fun readBytes(
        fileInputStream: FileInputStream,
        size: Int,
    ): ByteBuffer {
        val buffer = ByteArray(size)
        fileInputStream.read(buffer)

        val byteBuffer = ByteBuffer.wrap(buffer)
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)

        return byteBuffer
    }

    companion object {
        const val SIGNATURE: Int = 0x55AA1234
        const val NULL_TERMINATOR: Char = 0x0.toChar()

        const val MINIMUM_VERSION: Int = 1
        const val MAXIMUM_VERSION: Int = 2

        const val VERSION_ONE: Int = 1
        const val VERSION_TWO: Int = 2
        const val VERSION_ONE_HEADER_SIZE: Int = 12
        const val VERSION_TWO_HEADER_SIZE: Int = 28
    }
}
