package pl.shockah.iguana;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;

import javax.annotation.Nonnull;

import lombok.Getter;
import pl.shockah.jay.JSONObject;
import pl.shockah.jay.JSONParser;

public class Iguana {
	@Nonnull
	public static final File configFile = new File("config.json");

	@Getter
	private IguanaSession session;

	public static void main(String[] args) {
		try {
			JSONObject configJson = new JSONParser().parseObject(new String(Files.readAllBytes(configFile.toPath()), Charset.forName("UTF-8")));
			Configuration config = Configuration.read(configJson);
			new Iguana().start(config);
			//new Iguana().start();
		} catch (IOException | IguanaSession.Exception e) {
			e.printStackTrace();
		}
	}

	public void start(@Nonnull Configuration config) throws IguanaSession.Exception {
		session = new IguanaSession(config);
	}
}