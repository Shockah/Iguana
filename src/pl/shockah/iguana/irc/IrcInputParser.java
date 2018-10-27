package pl.shockah.iguana.irc;

import com.google.common.collect.ImmutableMap;

import org.pircbotx.Channel;
import org.pircbotx.InputParser;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.UserHostmask;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;

import lombok.AccessLevel;
import lombok.Getter;

public class IrcInputParser extends InputParser {
	@Getter(value = AccessLevel.PRIVATE, lazy = true)
	private final boolean availableAccountNotify = bot.getEnabledCapabilities().contains("account-notify");

	@Getter(value = AccessLevel.PRIVATE, lazy = true)
	private final boolean availableExtendedJoin = bot.getEnabledCapabilities().contains("extended-join");

	public IrcInputParser(@Nonnull PircBotX bot) {
		super(bot);
	}

	@Override
	public void processCommand(String target, UserHostmask source, String command, String line, List<String> parsedLine, ImmutableMap<String, String> map) throws IOException {
		Channel channel = (target.length() != 0 && configuration.getChannelPrefixes().indexOf(target.charAt(0)) >= 0 && bot.getUserChannelDao().containsChannel(target)) ? bot.getUserChannelDao().getChannel(target) : null;
		User sourceUser = bot.getUserChannelDao().containsUser(source) ? bot.getUserChannelDao().getUser(source) : null;

		if (command.equals("ACCOUNT")) {
			if (isAvailableAccountNotify()) {
				String account = parsedLine.get(0);
				if (account.equals("0") || account.equals("*"))
					account = null;
				if (sourceUser == null)
					throw new IllegalStateException();
				configuration.getListenerManager().onEvent(new AccountNotifyEvent(bot, sourceUser, account));
				return;
			}
		} else if (command.equals("JOIN")) {
			sourceUser = createUserIfNull(sourceUser, source);
			String sourceNick = source.getNick();
			if (!sourceNick.equalsIgnoreCase(bot.getNick())) {
				if (isAvailableExtendedJoin()) {
					String account = parsedLine.get(1);
					if (account.equals("0") || account.equals("*"))
						account = null;
					if (channel == null)
						throw new IllegalStateException();
					configuration.getListenerManager().onEvent(new ExtendedJoinEvent(bot, channel, sourceUser, account));
				}
			}
		}

		super.processCommand(target, source, command, line, parsedLine, map);
	}
}