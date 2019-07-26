package main;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class IOUtil {

	public static String read(final String filePath) throws IOException {
		final byte[] fileContent = Files.readAllBytes(Paths.get(filePath));
		final String content = new String(fileContent, Charset.forName("UTF-8"));
		return content;
	}

	public static List<String> readByLines(final String filePath) throws IOException {
		final List<String> lines = Files.readAllLines(Paths.get(filePath));
		return lines;
	}

	public static void write(final String filePath, final String fileContent) throws IOException {
		final Path path = Paths.get(filePath);
		Files.write(path, fileContent.getBytes());
	}

	public static void append(final String filePath, final String fileContent) throws IOException {
		final Path path = Paths.get(filePath);
		Files.write(path, fileContent.getBytes(), StandardOpenOption.APPEND);
	}
}
