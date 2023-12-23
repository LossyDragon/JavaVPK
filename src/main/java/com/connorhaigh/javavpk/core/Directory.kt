package com.connorhaigh.javavpk.core

@Suppress("unused")
class Directory(path: String) {
    /**
     * Returns the path of this directory.
     * @return the path
     */
    val path: String = path.trim()

    /**
     * Returns the list of entries in this directory.
     * @return the list of entries
     */
    val entries: ArrayList<Entry> = ArrayList()

    /**
     * Returns the full path for an entry in this directory.
     * @param entry the entry
     * @return the full path
     */
    fun getPathFor(entry: Entry): String = path + SEPARATOR + entry.fullName

    /**
     * Adds an entry to this directory.
     * @param entry the entry
     */
    fun addEntry(entry: Entry) {
        entries.add(entry)
    }

    /**
     * Removes an entry from this directory.
     * @param entry the entry
     */
    fun removeEntry(entry: Entry) {
        entries.remove(entry)
    }

    companion object {
        const val SEPARATOR: String = "/"
    }
}
