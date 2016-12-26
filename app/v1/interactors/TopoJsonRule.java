package v1.interactors;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

import play.Logger;
import play.libs.Json;
import dao.entities.Location;
import models.FeatureKey;
import models.Request;
import models.geo.FeatureCollection;

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

	private Path createTempFile(String topoJsonFilePrefix, String topoJsonFileSuffix) {
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
		req.setExcludedFeatureFields(Arrays.asList(new String[] { FeatureKey.CHILDREN.valueOf() }));
		FeatureCollection fc = GeoJsonRule.toFeatureCollection(locations, req);
		return fc;
	}

	private void topoJsonSystemCall(Path topoJsonFilePath, Path geoJsonFilePath) {
		String command = "topojson --no-quantization --properties --out " + topoJsonFilePath + " -- " + geoJsonFilePath;
		executeCommand(command);
	}

	private void executeCommand(String command) {
		String os = System.getProperty("os.name").toLowerCase();
		String[] cmdArr = new String[] {};
		if (os.indexOf("win") >= 0)
			cmdArr = new String[] { "cmd", "/c", command };
		if (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0 || os.indexOf("aix") > 0)
			cmdArr = new String[] { "bash", "-c", command };
		ProcessBuilder pb = new ProcessBuilder(cmdArr);
		try {
			Path logPath = createTempFile(logFilePath, "");
			pb.redirectErrorStream(true);
			pb.redirectOutput(Redirect.appendTo(logPath.toFile()));
			Process p = pb.start();
			p.waitFor();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
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

	private boolean deleteFiles(Path[] paths) {
		for (Path path : paths) {
			try {
				if (!Files.deleteIfExists(path)) {
					Logger.warn("could not delete " + path);
					return false;
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return true;

	}
}