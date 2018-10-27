package pl.shockah.iguana.irc;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.UserHostmask;
import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.types.GenericChannelUserEvent;

import lombok.Getter;

public class AccountNotifyEvent extends Event implements GenericChannelUserEvent {
	@Getter
	protected final Channel channel;

	@Getter
	protected final User user;

	@Getter
	protected final String account;

	public AccountNotifyEvent(PircBotX bot, Channel channel, User user, String account) {
		super(bot);
		this.channel = channel;
		this.user = user;
		this.account = account;
	}

	public UserHostmask getUserHostmask() {
		return bot.getConfiguration().getBotFactory().createUserHostmask(bot, user.getHostmask());
	}

	public void respond(String response) {
		getChannel().send().message(getUser(), response);
	}
}