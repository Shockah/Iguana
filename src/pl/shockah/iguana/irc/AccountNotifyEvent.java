package pl.shockah.iguana.irc;

import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.UserHostmask;
import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.types.GenericUserEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.Getter;

public class AccountNotifyEvent extends Event implements GenericUserEvent {
	@Nonnull
	@Getter
	protected final User user;

	@Nullable
	@Getter
	protected final String account;

	public AccountNotifyEvent(@Nonnull PircBotX bot, @Nonnull User user, @Nullable String account) {
		super(bot);
		this.user = user;
		this.account = account;
	}

	public UserHostmask getUserHostmask() {
		return bot.getConfiguration().getBotFactory().createUserHostmask(bot, user.getHostmask());
	}

	public void respond(String response) {
		throw new UnsupportedOperationException();
	}
}