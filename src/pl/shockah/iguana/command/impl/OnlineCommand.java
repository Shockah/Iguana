package pl.shockah.iguana.command.impl;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;

import org.pircbotx.Channel;
import org.pircbotx.User;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import lombok.Getter;
import pl.shockah.iguana.bridge.IrcChannelBridge;
import pl.shockah.iguana.command.ChannelCommand;
import pl.shockah.iguana.command.NamedCommand;

public class OnlineCommand implements ChannelCommand, NamedCommand {
	@Nonnull
	@Getter
	private final String name = "online";

	@Override
	public void execute(@Nonnull IrcChannelBridge channel, @Nonnull Message executingMessage, @Nonnull String input) {
		Channel ircChannel = channel.getIrcChannel();
		List<User> ircUsers = new ArrayList<>(ircChannel.getUsers());

		EmbedBuilder embedBuilder = new EmbedBuilder();
		embedBuilder.setTitle(String.format("Online users in %s", ircChannel.getName()));

		List<User> ops = ircUsers.stream()
				.filter(ircChannel::isOp)
				.collect(Collectors.toList());
		List<User> halfOps = ircUsers.stream()
				.filter(ircChannel::isHalfOp)
				.filter(((Predicate<User>)ops::contains).negate())
				.collect(Collectors.toList());
		List<User> voiced = ircUsers.stream()
				.filter(ircChannel::hasVoice)
				.filter(((Predicate<User>)ops::contains).negate())
				.filter(((Predicate<User>)halfOps::contains).negate())
				.collect(Collectors.toList());
		List<User> normalUsers = ircUsers.stream()
				.filter(((Predicate<User>)ops::contains).negate())
				.filter(((Predicate<User>)halfOps::contains).negate())
				.filter(((Predicate<User>)voiced::contains).negate())
				.collect(Collectors.toList());

		for (String opNicks : joinUserNicksLimitedTo1024(ops)) {
			embedBuilder.addField("Ops", opNicks, false);
		}
		for (String opNicks : joinUserNicksLimitedTo1024(halfOps)) {
			embedBuilder.addField("Half-ops", opNicks, false);
		}
		for (String opNicks : joinUserNicksLimitedTo1024(voiced)) {
			embedBuilder.addField("Voiced", opNicks, false);
		}
		for (String opNicks : joinUserNicksLimitedTo1024(normalUsers)) {
			embedBuilder.addField("Users", opNicks, false);
		}

		channel.getDiscordChannel().sendMessage(embedBuilder.build()).queue();
	}

	@Nonnull
	private List<String> joinUserNicksLimitedTo1024(@Nonnull List<User> users) {
		final String separator = ", ";

		List<String> result = new ArrayList<>();
		StringBuilder sb = new StringBuilder();
		for (User user : users) {
			String nick = user.getNick();
			if (sb.length() + nick.length() + separator.length() > 1024) {
				result.add(sb.toString());
				sb = new StringBuilder();
			}

			if (sb.length() != 0)
				sb.append(separator);
			sb.append(nick);
		}

		if (sb.length() != 0)
			result.add(sb.toString());
		return result;
	}
}