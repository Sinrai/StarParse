package com.ixale.starparse.ws;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpoint;
import javax.websocket.OnMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ixale.starparse.domain.Combat;
import com.ixale.starparse.domain.Event;
import com.ixale.starparse.domain.RaidRequest;
import com.ixale.starparse.domain.RaidRequest.Type;
import com.ixale.starparse.domain.stats.AbsorptionStats;
import com.ixale.starparse.domain.stats.ChallengeStats;
import com.ixale.starparse.domain.stats.CombatEventStats;
import com.ixale.starparse.domain.stats.CombatStats;
import com.ixale.starparse.gui.Config;

@ClientEndpoint(encoders = BaseEncoder.class, decoders = BaseDecoder.class, configurator = BaseClientConfigurator.class)
public class RaidClient extends BaseClient {

	private static final Logger logger = LoggerFactory.getLogger(RaidClient.class);

	public interface RaidResultHandler extends ResultHandler {

		void onPlayerJoin(String[] characterNames, String raidGroupName);

		void onPlayerQuit(String[] characterNames, String raidGroupName);

		void onCombatUpdated(RaidCombatMessage[] messages);

		void onRequestIncoming(RaidRequestMessage message, RequestIncomingCallback callback);
	}

	public interface RequestOutgoingCallback {

		void onResponseIncoming(RaidResponseMessage message);
	}

	public interface RequestIncomingCallback {

		void onResponseOutgoing(RaidRequest.Type reponseType, byte[] payload);
	}

	private final Map<String, RequestOutgoingCallback> pendingRequests = new HashMap<>();
	private final Map<String, CountDownLatch> pendingTimeouts = new HashMap<>();

	public RaidClient(Config config, final RaidResultHandler raidResultHandler) {
		super(config, Utils.ENDPOINT_RAID, raidResultHandler);
	}

	public void startRaiding(final String raidGroup, final String characterName, final Boolean storeEnabled) {

		sendMessage(new RaidPlayerMessage(RaidPlayerMessage.Action.JOIN, raidGroup, characterName, storeEnabled), true);
	}

	public void stopRaiding(final String raidGroup, String characterName) {

		sendMessage(new RaidPlayerMessage(RaidPlayerMessage.Action.QUIT, raidGroup, characterName, null), true);
	}

	public RaidCombatMessage updateCombat(final Combat combat, final String characterName,
		final CombatStats combatStats,
		final List<AbsorptionStats> absorptionStats,
		final List<ChallengeStats> challengeStats,
		final List<CombatEventStats> combatEventStats) {

		if (challengeStats != null && !challengeStats.isEmpty()) {
			// discard challenges with no value
			final Iterator<ChallengeStats> iter = challengeStats.iterator();
			while (iter.hasNext()) {
				ChallengeStats stats = iter.next();
				if ((stats.getDamage() == null || stats.getDamage() == 0)
					&& (stats.getEffectiveHeal() == null || stats.getEffectiveHeal() == 0)
					&& (stats.getHeal() == null || stats.getHeal() == 0)) {
					iter.remove();
				}
			}
		}

		// transform the very last event
		Event.Type exitEvent = null;
		List<CombatEventStats> combatEventStats2 = null;
		final long timestamp = combat.getTimeFrom() + combatStats.getTick();
		if (combatEventStats != null && !combatEventStats.isEmpty()) {
			final CombatEventStats last = combatEventStats.get(combatEventStats.size() - 1);
			if (last.getTimestamp() == timestamp) {
				exitEvent = last.getType();
				if (combatEventStats.size() > 1) {
					combatEventStats2 = new ArrayList<>(combatEventStats.subList(0, combatEventStats.size() - 1));
				}
			} else {
				combatEventStats2 = new ArrayList<>(combatEventStats);
			}
		}

		// fire and forget
		final RaidCombatMessage message = new RaidCombatMessage(characterName,
			combat.getTimeFrom(), combat.getTimeTo(),
			combat.getBoss() != null ? combat.getBoss().getRaidBossName() : null,
			combat.getBoss() != null ? combat.getBoss().getSize() : null,
			combat.getBoss() != null ? combat.getBoss().getMode() : null,
			combatStats, absorptionStats, challengeStats, combatEventStats2,
			timestamp, // last event timestamp
			combat.getDiscipline(),
			exitEvent);

		sendMessage(message, false);

		return message;
	}

	public void sendRequest(final RaidRequest request, final RequestOutgoingCallback callback) {
		final String guid = UUID.randomUUID().toString();

		final CountDownLatch responseLatch = new CountDownLatch(1);
		pendingRequests.put(guid, callback);
		pendingTimeouts.put(guid, responseLatch);

		final RaidRequestMessage message = new RaidRequestMessage(guid, request);
		if (logger.isDebugEnabled()) {
			logger.debug("Sending request: " + message);
		}

		sendMessage(message, false);

		final Thread timeout = new Thread("Timeout @ " + guid) {
			public void run() {
				boolean timedOut = true;
				try {
					timedOut = !responseLatch.await(25, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
				}

				if (timedOut) {
					if (logger.isDebugEnabled()) {
						logger.debug("Request timed out: " + message);
					}
					callback.onResponseIncoming(new RaidResponseMessage(guid, "Request timed out, server might be busy"));
				}
				pendingRequests.remove(responseLatch);
			};
		};
		timeout.setDaemon(true);
		timeout.start();
	}

	@OnMessage
	public void onMessage(final BaseMessage message) {
		if (message instanceof RaidPlayerMessage) {
			handlePlayerMessage((RaidPlayerMessage) message);
			return;
		}

		if (message instanceof RaidCombatMessageBatch) {
			handleCombatMessage(((RaidCombatMessageBatch) message).getMessages());
			return;
		}

		if (message instanceof RaidCombatMessage) {
			handleCombatMessage((RaidCombatMessage) message);
			return;
		}

		if (message instanceof RaidRequestMessage) {
			handleRequestMessage((RaidRequestMessage) message);
			return;
		}

		if (message instanceof RaidResponseMessage) {
			handleResponseMessage((RaidResponseMessage) message);
			return;
		}

		super.onMessage(message);
	}

	private void handlePlayerMessage(final RaidPlayerMessage message) {
		switch (message.getAction()) {
			case JOIN:
				getResultHandler().onPlayerJoin(message.getCharacterNames(), message.getRaidGroupName());
				break;
			case QUIT:
				getResultHandler().onPlayerQuit(message.getCharacterNames(), message.getRaidGroupName());
				break;
		}
	}

	private void handleCombatMessage(final RaidCombatMessage... messages) {
		getResultHandler().onCombatUpdated(messages);
	}

	private void handleRequestMessage(final RaidRequestMessage message) {
		if (logger.isDebugEnabled()) {
			logger.debug("Handling request: " + message);
		}
		getResultHandler().onRequestIncoming(message, new RequestIncomingCallback() {
			@Override
			public void onResponseOutgoing(final Type reponseType, final byte[] payload) {
				final RaidResponseMessage response = new RaidResponseMessage(message.getGuid(), reponseType, payload);
				if (logger.isDebugEnabled()) {
					logger.debug("Sending response: " + response);
				}
				// fire & forget
				sendMessage(response, false);
			}
		});
	}

	private void handleResponseMessage(final RaidResponseMessage message) {
		// lookup callback
		final RequestOutgoingCallback callback = message.getGuid() == null ? null : pendingRequests.get(message.getGuid());
		final CountDownLatch responseLatch = message.getGuid() == null ? null : pendingTimeouts.get(message.getGuid());
		if (callback != null) {
			callback.onResponseIncoming(message);
			if (logger.isDebugEnabled()) {
				logger.debug("Handling response: " + message);
			}
			pendingRequests.remove(callback);
		} else {
			// too bad, discard
			logger.warn("Discarding unknown response: " + message);
		}
		if (responseLatch != null) {
			responseLatch.countDown();
		}
	}

	protected RaidResultHandler getResultHandler() {
		return (RaidResultHandler) super.getResultHandler();
	}
}
