package pl.shockah.iguana.irc;

import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.events.WhoisEvent;

import lombok.Getter;
import lombok.experimental.Delegate;

public class Whois2Event extends Event {
	@Delegate
	protected final WhoisEvent event;

	@Getter
	protected final String operatorStatus;

	public Whois2Event(WhoisEvent event, String operatorStatus) {
		super(event.getBot());
		this.event = event;
		this.operatorStatus = operatorStatus;
	}
}