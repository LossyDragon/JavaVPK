package com.connorhaigh.javavpk.core

import com.connorhaigh.javavpk.exceptions.ArchiveException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * Returns the parent archive for this entry.
 * @return the parent archive
 *
 * @param archiveIndex Returns the child archive index this entry is contained in.
 * @param crc Returns the CRC checksum of this entry.
 * @param offset Returns the offset of this entry in the parent file.
 * @param length Returns the length of this entry, in bytes.
 * @param terminator Returns the terminator of this entry.
 */
@Suppress("MemberVisibilityCanBePrivate")
class Entry(
    val archive: Archive,
    val archiveIndex: Short,
    val preloadData: ByteArray?,
    filename: String,
    extension: String,
    val crc: Int,
    val offset: Int,
    val length: Int,
    val terminator: Short,
) {
    /**
     * Returns the full name of this entry.
     * @return the full name
     */
    val fullName: String
        get() = ("$fileName.$extension")

    /**
     * Returns the file name of this entry.
     * @return the file name
     */

    val fileName: String = filename.trim()

    /**
     * Returns the extension of this entry.
     * @return the extension
     */
    val extension: String = extension.trim()

    /**
     * Reads and returns the raw data for this entry.
     * If the entry has preload data, that is returned instead.
     * @return the raw data
     * @throws IOException if the entry could not be read
     * @throws ArchiveException if a general archive exception occurs
     */
    @Throws(IOException::class, ArchiveException::class)
    fun readData(): ByteArray {
        val data: ByteArray
        var dataOffset = 0

        // check for preload data
        if (preloadData != null) {
            if (length == 0) {
                return preloadData
            }

            // Allocate enough space for the data
            data = ByteArray(preloadData.size + length)
            // Copy in the preloaded data
            System.arraycopy(preloadData, 0, data, 0, preloadData.size)
            // If there's additional data let it know to insert after the preloaded data
            dataOffset = preloadData.size
        } else {
            data = ByteArray(length)
        }

        // get target archive
        val target: File =
            if (archive.isMultiPart) {
                archive.getChildArchive(archiveIndex.toInt())
            } else {
                archive.file
            }

        FileInputStream(target).use { fileInputStream ->
            if (archiveIndex.toInt() == TERMINATOR) {
                // skip tree and header
                fileInputStream.skip(archive.treeLength.toLong())
                fileInputStream.skip(archive.headerLength.toLong())
            }

            // read data
            fileInputStream.skip(offset.toLong())
            fileInputStream.read(data, dataOffset, length)

            return data
        }
    }

    /**
     * Extracts the data from this entry to the specified file.
     * @param file the file to extract to
     * @throws IOException if the entry could not be read
     * @throws ArchiveException if a general archive exception occurs
     */
    @Throws(IOException::class, ArchiveException::class)
    fun extract(file: File) {
        FileOutputStream(file).use { fileOutputStream ->
            fileOutputStream.write(readData())
        }
    }

    companion object {
        const val TERMINATOR: Int = 0x7FFF
    }
}
