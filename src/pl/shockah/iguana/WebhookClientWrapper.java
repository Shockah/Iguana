package pl.shockah.iguana;

import net.dv8tion.jda.webhook.WebhookClient;

import javax.annotation.Nonnull;

import lombok.Getter;
import lombok.experimental.Delegate;

public class WebhookClientWrapper implements AutoCloseable {
	@Nonnull
	@Getter
	@Delegate
	private final WebhookClient client;

	public WebhookClientWrapper(@Nonnull WebhookClient client) {
		this.client = client;
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		client.close();
	}
}