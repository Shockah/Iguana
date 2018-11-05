package pl.shockah.iguana.bridge;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import org.pircbotx.Channel;
import org.pircbotx.User;
import org.pircbotx.UserHostmask;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.KickEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.ModeEvent;
import org.pircbotx.hooks.events.NickChangeEvent;
import org.pircbotx.hooks.events.NoticeEvent;
import org.pircbotx.hooks.events.PartEvent;
import org.pircbotx.hooks.events.QuitEvent;
import org.pircbotx.hooks.events.RemoveChannelBanEvent;
import org.pircbotx.hooks.events.SetChannelBanEvent;
import org.pircbotx.hooks.events.TopicEvent;
import org.pircbotx.hooks.events.UserModeEvent;

import java.time.Instant;

import javax.annotation.Nonnull;

import lombok.Getter;
import pl.shockah.iguana.Configuration;
import pl.shockah.iguana.WebhookClientWrapper;
import pl.shockah.iguana.command.ChannelCommand;
import pl.shockah.iguana.command.CommandCall;
import pl.shockah.iguana.format.irc.IrcFormattingConstants;

public class IrcChannelBridge extends IrcMessageBridge {
	@Nonnull
	@Getter
	private final Configuration.IRC.Server.Channel ircChannelConfig;

	@Nonnull
	@Getter(lazy = true)
	private final TextChannel discordChannel = ircChannelConfig.getDiscordChannel(session.getDiscord());

	@Nonnull
	@Getter(lazy = true)
	private final TextChannel discordEventChannel = ircChannelConfig.getDiscordEventChannel(session.getDiscord());

	@Nonnull
	@Getter(lazy = true)
	private final Channel ircChannel = ircBot.getUserChannelDao().getChannel(ircChannelConfig.getName());

	public IrcChannelBridge(@Nonnull IrcServerBridge serverBridge, @Nonnull Configuration.IRC.Server.Channel ircChannelConfig) {
		super(serverBridge);
		this.ircChannelConfig = ircChannelConfig;
	}

	@Nonnull
	@Override
	protected WebhookClientWrapper initializeWebhookClient() {
		return new WebhookClientWrapper(ircChannelConfig.getWebhook(session.getDiscord()).newClient().build());
	}

	@Nonnull
	@Override
	protected String formatNick(@Nonnull User user) {
		String nickname = user.getNick();
		if (getIrcChannel().hasVoice(user))
			nickname = String.format("+%s", nickname);
		else if (getIrcChannel().isHalfOp(user))
			nickname = String.format("%%%s", nickname);
		else if (getIrcChannel().isOp(user))
			nickname = String.format("@%s", nickname);
		return nickname;
	}

	public void onChannelMessage(@Nonnull MessageEvent event) {
		Channel channel = event.getChannel();
		if (channel == null)
			return;

		User user = event.getUser();
		if (user == null)
			return;

		onMessage(user, event.getMessage());
	}

	public void onChannelAction(@Nonnull ActionEvent event) {
		Channel channel = event.getChannel();
		if (channel == null)
			return;

		User user = event.getUser();
		if (user == null)
			return;

		String italicizedMessage = event.getMessage();
		italicizedMessage = italicizedMessage.replace(IrcFormattingConstants.RESET, IrcFormattingConstants.RESET + IrcFormattingConstants.ITALIC);
		italicizedMessage = IrcFormattingConstants.ITALIC + italicizedMessage;
		onMessage(user, italicizedMessage);
	}

	public void onChannelNotice(@Nonnull NoticeEvent event) {
		Channel channel = event.getChannel();
		if (channel == null)
			return;

		User user = event.getUser();
		if (user == null)
			return;

		onMessage(user, event.getMessage(), true);
	}

	public void onSelfJoin(@Nonnull JoinEvent event) {
		Channel channel = event.getChannel();
		if (channel == null)
			return;

		if (!channel.getTopic().equals(getDiscordChannel().getTopic()))
			getDiscordChannel().getManager().setTopic(channel.getTopic()).queue();
	}

	public void onJoin(@Nonnull JoinEvent event) {
		User user = event.getUser();
		if (user == null)
			return;

		getDiscordEventChannel().sendMessage(
				new EmbedBuilder()
						.setColor(session.getConfiguration().appearance.channelEvents.getJoinColor())
						.setDescription(String.format("**%s** (`%s!%s@%s`) joined.", user.getNick(), user.getNick(), user.getLogin(), user.getHostname()))
						.setTimestamp(Instant.now())
						.build()
		).queue();
	}

	public void onPart(@Nonnull PartEvent event) {
		User user = event.getUser();
		if (user == null)
			return;

		getDiscordEventChannel().sendMessage(
				new EmbedBuilder()
						.setColor(session.getConfiguration().appearance.channelEvents.getLeaveColor())
						.setDescription(String.format(
								"%s left (%s).",
								session.getBridge().getDiscordFormattedFullUserInfo(user),
								event.getReason()
						))
						.setTimestamp(Instant.now())
						.build()
		).queue();
	}

	public void onQuit(@Nonnull QuitEvent event) {
		User user = event.getUser();
		if (user == null)
			return;

		getDiscordEventChannel().sendMessage(
				new EmbedBuilder()
						.setColor(session.getConfiguration().appearance.channelEvents.getLeaveColor())
						.setDescription(String.format(
								"%s has quit (%s).",
								session.getBridge().getDiscordFormattedFullUserInfo(user),
								event.getReason()
						))
						.setTimestamp(Instant.now())
						.build()
		).queue();
	}

	public void onNickChange(@Nonnull NickChangeEvent event) {
		User user = event.getUser();
		if (user == null)
			return;

		getDiscordEventChannel().sendMessage(
				new EmbedBuilder()
						.setColor(session.getConfiguration().appearance.channelEvents.getNickChangeColor())
						.setDescription(String.format(
								"**%s** is now known as %s.",
								event.getOldNick(),
								session.getBridge().getDiscordFormattedFullUserInfo(user)
						))
						.setTimestamp(Instant.now())
						.build()
		).queue();
	}

	public void onTopic(@Nonnull TopicEvent event) {
		Channel channel = event.getChannel();
		if (channel == null)
			return;

		UserHostmask user = event.getUser();
		if (user == null)
			return;

		getDiscordChannel().getManager().setTopic(event.getTopic()).queue();

		getDiscordChannel().sendMessage(
				new EmbedBuilder()
						.setColor(session.getConfiguration().appearance.channelEvents.getTopicColor())
						.setDescription(String.format(
								"%s changed the topic.",
								session.getBridge().getDiscordFormattedHostmask(user)
						))
						.addField("Old", event.getOldTopic().equals("") ? "<empty>" : event.getOldTopic(), false)
						.addField("New", event.getTopic().equals("") ? "<empty>" : event.getTopic(), false)
						.setTimestamp(Instant.now())
						.build()
		).queue();
	}

	public void onKick(@Nonnull KickEvent event) {
		User user = event.getUser();
		if (user == null)
			return;
		User recipient = event.getRecipient();
		if (recipient == null)
			return;

		getDiscordChannel().sendMessage(
				new EmbedBuilder()
						.setColor(session.getConfiguration().appearance.channelEvents.getKickColor())
						.setDescription(String.format(
								"%s kicked %s.",
								session.getBridge().getDiscordFormattedFullUserInfo(user),
								session.getBridge().getDiscordFormattedFullUserInfo(recipient)
						))
						.addField("Reason", event.getReason(), false)
						.setTimestamp(Instant.now())
						.build()
		).queue();
	}

	public void onSetChannelBan(@Nonnull SetChannelBanEvent event) {
		User user = event.getUser();
		if (user == null)
			return;

		getDiscordChannel().sendMessage(
				new EmbedBuilder()
						.setColor(session.getConfiguration().appearance.channelEvents.getBanColor())
						.setDescription(String.format(
								"%s banned %s.",
								session.getBridge().getDiscordFormattedFullUserInfo(user),
								session.getBridge().getDiscordFormattedHostmask(event.getBanHostmask())
						))
						.setTimestamp(Instant.now())
						.build()
		).queue();
	}

	public void onRemoveChannelBan(@Nonnull RemoveChannelBanEvent event) {
		User user = event.getUser();
		if (user == null)
			return;

		getDiscordChannel().sendMessage(
				new EmbedBuilder()
						.setColor(session.getConfiguration().appearance.channelEvents.getUnbanColor())
						.setDescription(String.format(
								"%s unbanned %s.",
								session.getBridge().getDiscordFormattedFullUserInfo(user),
								session.getBridge().getDiscordFormattedHostmask(event.getHostmask())
						))
						.setTimestamp(Instant.now())
						.build()
		).queue();
	}

	public void onMode(@Nonnull ModeEvent event) {
		User user = event.getUser();
		if (user == null)
			return;

		getDiscordChannel().sendMessage(
				new EmbedBuilder()
						.setColor(session.getConfiguration().appearance.channelEvents.getModeColor())
						.setDescription(String.format(
								"%s set `%s`.",
								session.getBridge().getDiscordFormattedFullUserInfo(user),
								event.getMode()
						))
						.setTimestamp(Instant.now())
						.build()
		).queue();
	}

	public void onUserMode(@Nonnull UserModeEvent event) {
		User user = event.getUser();
		if (user == null)
			return;
		User recipient = event.getRecipient();
		if (recipient == null)
			return;

		getDiscordChannel().sendMessage(
				new EmbedBuilder()
						.setColor(session.getConfiguration().appearance.channelEvents.getModeColor())
						.setDescription(String.format(
								"%s set `%s` on %s.",
								session.getBridge().getDiscordFormattedFullUserInfo(user),
								event.getMode(),
								session.getBridge().getDiscordFormattedFullUserInfo(recipient)
						))
						.setTimestamp(Instant.now())
						.build()
		).queue();
	}

	public void onDiscordMessage(@Nonnull GuildMessageReceivedEvent event) {
		String discordMessage = event.getMessage().getContentDisplay().trim();
		if (!discordMessage.equals("")) {
			CommandCall call = getSession().getBridge().getCommandManager().parseCommandCall(discordMessage);
			if (call != null) {
				ChannelCommand command = getSession().getBridge().getCommandManager().getCommandForChannelContext(call.commandName);
				if (command != null) {
					command.execute(this, event.getMessage(), call.input);
					return;
				}
			}

			String ircMessage = session.getIrcFormatter().output(session.getDiscordFormatter().parse(discordMessage, null), null);
			getIrcChannel().send().message(ircMessage);
		}

		for (Message.Attachment attachment : event.getMessage().getAttachments()) {
			String url = attachment.getUrl();
			if (url != null && !url.equals(""))
				getIrcChannel().send().message(String.format("[ Attachment: %s ]", url));
		}
	}
}