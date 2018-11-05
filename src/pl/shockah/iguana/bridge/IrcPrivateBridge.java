package pl.shockah.iguana.bridge;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import org.pircbotx.Channel;
import org.pircbotx.User;
import org.pircbotx.exception.DaoException;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.NoticeEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.Getter;
import pl.shockah.iguana.Configuration;
import pl.shockah.iguana.WebhookClientWrapper;
import pl.shockah.iguana.command.CommandCall;
import pl.shockah.iguana.command.UserCommand;
import pl.shockah.iguana.format.irc.IrcFormattingConstants;

public class IrcPrivateBridge extends IrcMessageBridge {
	@Nonnull
	@Getter
	private final Configuration.IRC.Server.Private ircPrivateConfig;

	@Nonnull
	@Getter(lazy = true)
	private final TextChannel discordChannel = ircPrivateConfig.getDiscordChannel(session.getDiscord());

	public IrcPrivateBridge(@Nonnull IrcServerBridge serverBridge, @Nonnull Configuration.IRC.Server.Private ircPrivateConfig) {
		super(serverBridge);
		this.ircPrivateConfig = ircPrivateConfig;
	}

	@Nonnull
	@Override
	protected WebhookClientWrapper initializeWebhookClient() {
		return new WebhookClientWrapper(ircPrivateConfig.getWebhook(session.getDiscord()).newClient().build());
	}

	@Nullable
	public User getCurrentUser() {
		if (ircPrivateConfig.isNickServAccount()) {
			return getServerBridge().nickServIdentityManager.getUserForAccount(ircPrivateConfig.getName());
		} else {
			try {
				return ircBot.getUserChannelDao().getUser(ircPrivateConfig.getName());
			} catch (DaoException e) {
				return null;
			}
		}
	}

	public void onPrivateMessage(@Nonnull PrivateMessageEvent event) {
		User user = event.getUser();
		if (user == null)
			return;

		onMessage(user, event.getMessage());
	}

	public void onPrivateAction(@Nonnull ActionEvent event) {
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

	public void onPrivateNotice(@Nonnull NoticeEvent event) {
		Channel channel = event.getChannel();
		if (channel == null)
			return;

		User user = event.getUser();
		if (user == null)
			return;

		onMessage(user, event.getMessage(), true);
	}

	public void onDiscordMessage(@Nonnull GuildMessageReceivedEvent event) {
		User user = getCurrentUser();
		if (user == null) {
			getDiscordChannel().sendMessage(new EmbedBuilder()
					.setDescription("User is currently offline.")
					.build()).queue();
			return;
		}

		String discordMessage = event.getMessage().getContentDisplay().trim();
		if (!discordMessage.equals("")) {
			CommandCall call = getSession().getBridge().getCommandManager().parseCommandCall(discordMessage);
			if (call != null) {
				UserCommand command = getSession().getBridge().getCommandManager().getCommandForUserContext(call.commandName);
				if (command != null) {
					command.execute(this, event.getMessage(), call.input);
					return;
				}
			}

			String ircMessage = session.getIrcFormatter().output(session.getDiscordFormatter().parse(discordMessage, null), null);
			user.send().message(ircMessage);
		}

		for (Message.Attachment attachment : event.getMessage().getAttachments()) {
			String url = attachment.getUrl();
			if (url != null && !url.equals(""))
				user.send().message(String.format("[ Attachment: %s ]", url));
		}
	}
}