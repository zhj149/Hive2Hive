package org.hive2hive.core.extras.buffer;

import java.io.File;
import java.util.List;
import java.util.Set;

import org.hive2hive.core.extras.Extra;
import org.hive2hive.core.processes.files.list.FileTaste;

@Extra
public interface IFileBufferHolder {

	/**
	 * Get the list of files which are in sync with the DHT (use it to filter your files in the buffer)
	 */
	public Set<FileTaste> getSyncFiles();

	/**
	 * Get the list of files in the buffer
	 */
	public List<File> getFileBuffer();
}