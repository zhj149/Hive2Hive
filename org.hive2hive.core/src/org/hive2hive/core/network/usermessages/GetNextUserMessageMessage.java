package org.hive2hive.core.network.usermessages;

import org.hive2hive.core.log.H2HLogger;
import org.hive2hive.core.log.H2HLoggerFactory;
import org.hive2hive.core.network.messages.AcceptanceReply;
import org.hive2hive.core.network.messages.request.RoutedRequestMessage;

/**
 * A routed message to the proxy node of a user in order to get the next user message that should be handled.
 * 
 * @author Christian
 * 
 */
@Deprecated
public class GetNextUserMessageMessage extends RoutedRequestMessage {

	// TODO this might actually not be a user message
	private static final H2HLogger logger = H2HLoggerFactory.getLogger(GetNextUserMessageMessage.class);

	private static final long serialVersionUID = 580669795666539208L;

	public GetNextUserMessageMessage(String targetKey) {
		super(targetKey);
	}

	@Override
	public synchronized void run() {

		// TODO do the user message get here
		
//		// load the next user message
//		UserMessageQueue umQueue = (UserMessageQueue) networkManager.getDataManager().getLocal(targetKey,
//				H2HConstants.USER_MESSAGE_QUEUE_KEY);
//
//		if (umQueue != null) {
//			SerializableLinkedList<BaseMessage> messageQueue = umQueue.getMessageQueue();
//			NextFromQueueResponse responseObject = new NextFromQueueResponse(
//					(BaseMessage) messageQueue.poll(), messageQueue.size());
//
//			sendDirectResponse(createResponse(responseObject));
//		} else {
//			// TODO return a correct failure message
//			logger.error("The UserMessageQueue could not be loaded and returned null.");
//		}
	}

	@Override
	public AcceptanceReply accept() {
		// TODO check!
		return AcceptanceReply.OK;
	}
}