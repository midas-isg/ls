package interactors;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import com.fasterxml.jackson.databind.JsonNode;

import dao.entities.Location;
import models.FeatureKey;
import models.Request;
import models.geo.FeatureCollection;
import play.Logger;
import play.libs.Json;

public class TopoJsonRule {

	private String geoJsonFilePrefix = "geoJSON_";
	private String geoJsonFileSuffix = ".geojson";
	private String topoJsonFilePrefix = "topoJSON_";
	private String topoJsonFileSuffix = ".topojson";

	private String logFilePath = "topojson_log_";

	public String toTopoJson(List<Long> gids) {
		if (gids == null)
			return null;
		FeatureCollection fc = toFeatureCollection(gids);
		return toTopoJson(fc);
	}

	private String toTopoJson(FeatureCollection fc) {
		Path geoJsonFilePath = toGeoJsonFile(fc, geoJsonFilePrefix, geoJsonFileSuffix);
		Path topoJsonFilePath = createTempFile(topoJsonFilePrefix, topoJsonFileSuffix);
		String topoJson = toTopoJson(topoJsonFilePath, geoJsonFilePath);
		deleteFiles(new Path[] { topoJsonFilePath, geoJsonFilePath });
		return topoJson;
	}

	private static Path createTempFile(String topoJsonFilePrefix, String topoJsonFileSuffix) {
		Path topoJsonFilePath;
		try {
			topoJsonFilePath = Files.createTempFile(topoJsonFilePrefix, topoJsonFileSuffix, new FileAttribute<?>[0]);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return topoJsonFilePath;
	}

	String toTopoJson(Path topoJsonFilePath, Path geoJsonFilePath) {

		topoJsonSystemCall(topoJsonFilePath, geoJsonFilePath);
		String result = readFileContent(topoJsonFilePath);
		return result;
	}

	private FeatureCollection toFeatureCollection(List<Long> gids) {

		List<Location> locations = new ArrayList<>();
		Set<Long> uniqueGids = new HashSet<>();
		uniqueGids.addAll(gids);
		for (Long gid : uniqueGids)
			locations.add(LocationRule.read(gid));

		Request req = new Request();
		req.setExcludedFeatureFields(Arrays.asList(new String[] { FeatureKey.CHILDREN }));
		FeatureCollection fc = GeoJsonRule.toFeatureCollection(locations, req);
		return fc;
	}

	private void topoJsonSystemCall(Path topoJsonFilePath, Path geoJsonFilePath) {
		String command = toTopoJsonCommand(topoJsonFilePath, geoJsonFilePath);
		executeCommand(command, logFilePath);
	}

	String toTopoJsonCommand(Path topoJsonFilePath, Path geoJsonFilePath) {
		return "topojson --no-quantization --properties --out " + topoJsonFilePath + " -- " + geoJsonFilePath;
	}

	private void executeCommand(String command, String logPath) {
		String[] cmdArr = toCommandArray(command);
		execute(cmdArr, logPath);
	}

	public static String execute(String[] cmdArr, String logPath) {
		String result;
		ProcessBuilder pb = new ProcessBuilder(cmdArr);
		try {
			pb.redirectErrorStream(true);
			if (logPath != null) {
				Path path = createTempFile(logPath, "");
				pb.redirectOutput(Redirect.appendTo(path.toFile()));
			}
			Process p = pb.start();
			result = getProcessOutput(p);
			p.waitFor();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	private static String getProcessOutput(Process p) {
		String result;
		final BufferedReader output = new BufferedReader(new InputStreamReader(p.getInputStream()));
		StringJoiner sj = new StringJoiner(System.getProperty("line.separator"));
		output.lines().iterator().forEachRemaining(sj::add);
		result = sj.toString();
		return result;
	}

	public static String[] toCommandArray(String command) {
		String os = System.getProperty("os.name").toLowerCase();
		String[] cmdArr = new String[] {};
		if (os.indexOf("win") >= 0)
			cmdArr = new String[] { "cmd", "/c", command };
		if (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0 || os.indexOf("aix") > 0)
			cmdArr = new String[] { "bash", "-c", command };
		return cmdArr;
	}

	private Path toGeoJsonFile(FeatureCollection fc, String prefix, String suffix) {
		Path outPath;
		JsonNode node = Json.toJson(fc);
		String stringFc = node.toString();
		Path path = createTempFile(prefix, suffix);
		try {
			outPath = Files.write(path, stringFc.getBytes(UTF_8));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return outPath;
	}

	String readFileContent(Path path) {
		byte[] bytes;
		try {
			bytes = Files.readAllBytes(path);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return new String(bytes);
	}

	private void deleteFiles(Path[] paths) {
		for (Path path : paths) {
			try {
				if (!Files.deleteIfExists(path)) {
					Logger.warn("could not delete " + path);
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
}