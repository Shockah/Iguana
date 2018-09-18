package pl.shockah.iguana;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;

import javax.annotation.Nonnull;

import lombok.Getter;
import pl.shockah.jay.JSONObject;
import pl.shockah.jay.JSONParser;
import pl.shockah.jay.JSONPrettyPrinter;

public class Iguana {
	@Nonnull
	public static final File configFile = new File("config.json");

	@Getter
	private IguanaSession session;

	public static void main(String[] args) {
		try {
			Iguana app = new Iguana();
			app.start(Configuration.read(app.loadConfigJson()));
		} catch (IOException | IguanaSession.Exception e) {
			e.printStackTrace();
		}
	}

	public void start(@Nonnull Configuration config) throws IguanaSession.Exception {
		session = new IguanaSession(this, config);
	}

	@Nonnull
	public JSONObject loadConfigJson() throws IOException {
		return new JSONParser().parseObject(new String(Files.readAllBytes(configFile.toPath()), Charset.forName("UTF-8")));
	}

	public void saveConfigJson(@Nonnull JSONObject configJson) throws IOException {
		Files.write(configFile.toPath(), new JSONPrettyPrinter().toString(configJson).getBytes(Charset.forName("UTF-8")));
	}
}