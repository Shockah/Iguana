package pl.shockah.iguana.irc;

import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.UserHostmask;
import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.types.GenericMessageEvent;

import lombok.Getter;

public class ServerNoticeEvent extends Event implements GenericMessageEvent {
	@Getter
	protected final User user;

	@Getter
	protected final UserHostmask userHostmask;

	@Getter
	protected final String message;

	public ServerNoticeEvent(PircBotX bot, UserHostmask userHostmask, User user, String message) {
		super(bot);
		this.user = user;
		this.userHostmask = userHostmask;
		this.message = message;
	}

	public void respond(String response) {
		throw new UnsupportedOperationException();
	}

	public void respondPrivateMessage(String response) {
		throw new UnsupportedOperationException();
	}

	public void respondWith(String response) {
		throw new UnsupportedOperationException();
	}
}