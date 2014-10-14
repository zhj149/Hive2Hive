package org.hive2hive.core.processes.common.base;

import java.security.KeyPair;
import java.security.PublicKey;

import org.hive2hive.core.exceptions.PutFailedException;
import org.hive2hive.core.model.BaseNetworkContent;
import org.hive2hive.core.model.versioned.BaseVersionedNetworkContent;
import org.hive2hive.core.network.data.DataManager;
import org.hive2hive.core.network.data.DataManager.H2HPutStatus;
import org.hive2hive.core.network.data.parameters.IParameters;
import org.hive2hive.core.network.data.parameters.Parameters;
import org.hive2hive.core.security.H2HDefaultEncryption;
import org.hive2hive.processframework.RollbackReason;
import org.hive2hive.processframework.abstracts.ProcessStep;
import org.hive2hive.processframework.exceptions.InvalidProcessStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class for {@link ProcessStep}s that intend to PUT content to the DHT.
 * 
 * @author Christian, Seppi
 */
public abstract class BasePutProcessStep extends ProcessStep {

	private static final Logger logger = LoggerFactory.getLogger(BasePutProcessStep.class);

	protected final DataManager dataManager;
	protected boolean putPerformed;

	private IParameters parameters;

	public BasePutProcessStep(DataManager dataManager) {
		this.dataManager = dataManager;
	}

	protected void put(PublicKey locationKey, String contentKey, BaseNetworkContent content, KeyPair protectionKey)
			throws PutFailedException {
		put(H2HDefaultEncryption.key2String(locationKey), contentKey, content, protectionKey);
	}

	protected void put(String locationKey, String contentKey, BaseNetworkContent content, KeyPair protectionKeys)
			throws PutFailedException {
		Parameters params = new Parameters().setLocationKey(locationKey).setContentKey(contentKey)
				.setNetworkContent(content).setProtectionKeys(protectionKeys).setTTL(content.getTimeToLive());
		put(params);
	}

	protected void put(String locationKey, String contentKey, BaseVersionedNetworkContent content, KeyPair protectionKeys)
			throws PutFailedException {
		Parameters params = new Parameters().setLocationKey(locationKey).setContentKey(contentKey)
				.setVersionKey(content.getVersionKey()).setBasedOnKey(content.getBasedOnKey()).setNetworkContent(content)
				.setProtectionKeys(protectionKeys).setTTL(content.getTimeToLive());
		put(params);
	}

	protected void put(IParameters parameters) throws PutFailedException {
		// store for roll back
		this.parameters = parameters;

		H2HPutStatus status = dataManager.put(parameters);
		putPerformed = true;

		if (!status.equals(H2HPutStatus.OK)) {
			throw new PutFailedException();
		}
	}

	@Override
	protected void doRollback(RollbackReason reason) throws InvalidProcessStateException {
		if (!putPerformed) {
			logger.warn("Nothing to remove at rollback because nothing has been put.");
			return;
		}

		boolean success = dataManager.removeVersion(parameters);
		if (success) {
			logger.debug("Rollback of put succeeded. '{}'", parameters.toString());
		} else {
			logger.warn("Rollback of put failed. Remove failed. '{}'", parameters.toString());
		}
	}
}