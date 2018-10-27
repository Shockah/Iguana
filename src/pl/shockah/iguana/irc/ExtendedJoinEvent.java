package pl.shockah.iguana.irc;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.UserHostmask;
import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.types.GenericChannelUserEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.Getter;

public class ExtendedJoinEvent extends Event implements GenericChannelUserEvent {
	@Nonnull
	@Getter
	protected final Channel channel;

	@Nonnull
	@Getter
	protected final User user;

	@Nullable
	@Getter
	protected final String account;

	public ExtendedJoinEvent(@Nonnull PircBotX bot, @Nonnull Channel channel, @Nonnull User user, @Nullable String account) {
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