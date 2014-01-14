package org.hive2hive.core.process.upload;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import org.apache.log4j.Logger;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.hive2hive.core.H2HConstants;
import org.hive2hive.core.IFileConfiguration;
import org.hive2hive.core.file.FileUtil;
import org.hive2hive.core.log.H2HLoggerFactory;
import org.hive2hive.core.model.Chunk;
import org.hive2hive.core.process.ProcessStep;
import org.hive2hive.core.process.common.put.BasePutProcessStep;
import org.hive2hive.core.security.EncryptionUtil;
import org.hive2hive.core.security.H2HEncryptionUtil;
import org.hive2hive.core.security.HybridEncryptedContent;

/**
 * First validates the file size according to the limits set in the {@link IFileConfiguration} object.
 * Then, puts a chunk and recursively calls itself until all chunks are stored in the DHT.
 * This class is intended to be subclassed because there are two scenarios:
 * <ul>
 * <li>A new file is uploaded</li>
 * <li>A new version of an existing is uploaded</li>
 * </ul>
 * The upload itself is not differing in these scenarios, but the subsequent steps might be.
 * 
 * @author Nico
 * 
 */
public class PutChunkStep extends BasePutProcessStep {

	private final static Logger logger = H2HLoggerFactory.getLogger(PutChunkStep.class);

	private final long offset;
	private final ProcessStep stepAfterPutting;

	public PutChunkStep(ProcessStep stepAfterPutting) {
		super(null);
		this.offset = 0;
		this.stepAfterPutting = stepAfterPutting;
	}
	
	/**
	 * Constructor only usable with subclass. Remember to configure the steps after uploading before starting
	 * this step. This ensures that the step knows what to do when all parts are uploaded.
	 * 
	 * @param file the file to upload
	 * @param offset how many bytes of the file are already on the network
	 * @param chunkKeys the collected chunk keys during upload. This also indicates the progress of the
	 *            chunking
	 */
	protected PutChunkStep(long offset, ProcessStep stepAfterPutting) {
		// the details are set later
		super(null);
		this.offset = offset;
		this.stepAfterPutting = stepAfterPutting;
	}

	// TODO create 2nd constructor (private) for better readability and safety

	@Override
	public void start() {
		UploadFileProcessContext context = (UploadFileProcessContext) getProcess().getContext();
		List<KeyPair> chunkKeys = context.getChunkKeys();
		File file = context.getFile();
		
		// only put sth. if the file has content
		if (file.isDirectory()) {
			logger.debug("File " + file.getName() + ": No data to put because the file is a folder");
			getProcess().setNextStep(stepAfterPutting);
			return;
		}

		// first, validate the file size (only first time)
		if (chunkKeys.isEmpty()) {
			IFileConfiguration config = context.getH2HSession().getFileConfiguration();
			long fileSize = FileUtil.getFileSize(file);

			if (fileSize > config.getMaxFileSize()) {
				getProcess().stop("File is too large");
				return;
			}
		}

		byte[] data = new byte[context.getH2HSession().getFileConfiguration().getChunkSize()];
		int read = -1;
		try {
			// read the next chunk of the file considering the offset
			RandomAccessFile rndAccessFile = new RandomAccessFile(file, "r");
			rndAccessFile.seek(offset);
			read = rndAccessFile.read(data);
			rndAccessFile.close();
		} catch (IOException e) {
			logger.error("File " + file.getAbsolutePath() + ": Could not read the file", e);
			getProcess().stop(e.getMessage());
			return;
		}

		if (read > 0) {
			// create a chunk

			// the byte-Array may contain many empty slots if last chunk. Truncate it
			data = truncateData(data, read);

			KeyPair chunkKey = EncryptionUtil.generateRSAKeyPair(H2HConstants.KEYLENGTH_CHUNK);
			Chunk chunk = new Chunk(chunkKey.getPublic(), data, chunkKeys.size(), read);
			chunkKeys.add(chunkKey);

			// more data to read (increase offset)
			PutChunkStep nextChunkStep = new PutChunkStep(offset + data.length, stepAfterPutting);
			nextStep = nextChunkStep;

			try {
				// encrypt the chunk prior to put such that nobody can read it
				HybridEncryptedContent encryptedContent = H2HEncryptionUtil.encryptHybrid(chunk,
						chunkKey.getPublic());

				// start the put and continue with next chunk
				logger.debug("Uploading chunk " + chunk.getOrder() + " of file " + file.getName());
				put(key2String(chunk.getId()), H2HConstants.FILE_CHUNK, encryptedContent, context.getProtectionKeys());
			} catch (DataLengthException | InvalidKeyException | IllegalStateException
					| InvalidCipherTextException | IllegalBlockSizeException | BadPaddingException e) {
				logger.error("Could not encrypt the chunk", e);
				getProcess().stop(e);
			}
		} else {
			logger.debug("File " + file.getName() + ": All chunks uploaded. Continue with meta data.");
			// nothing read, stop putting chunks and start next step
			getProcess().setNextStep(stepAfterPutting);
		}
	}

	/**
	 * Truncates a byte array
	 * 
	 * @param data
	 * @param read
	 * @return a shorter byte array
	 */
	private byte[] truncateData(byte[] data, int numOfBytes) {
		// shortcut
		if (data.length == numOfBytes) {
			return data;
		} else {
			byte[] truncated = new byte[numOfBytes];
			for (int i = 0; i < truncated.length; i++) {
				truncated[i] = data[i];
			}
			return truncated;
		}
	}

}
