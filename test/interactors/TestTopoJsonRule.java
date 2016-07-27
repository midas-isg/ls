package interactors;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

public class TestTopoJsonRule {

	private Path testTopoJsonFile = Paths.get("test/resources/test/topoJSON.topojson");
	private Path testFeatureCollectionFile = Paths.get("test/resources/test/FeatureCollection.geojson");
	private Path tempTopoJsonFile = Paths.get(System.getProperty("java.io.tmpdir") + "tempTopoJson.topojson");

	@Test
	public void testGeoJsonFile2TopoJson() {
		TopoJsonRule tjRule = new TopoJsonRule();
		String actualTopoJson = tjRule.toTopoJson(tempTopoJsonFile, testFeatureCollectionFile);
		String expectedTopoJson = tjRule.readFileContent(testTopoJsonFile);
		deleteFile(tempTopoJsonFile);
		assertEquals(expectedTopoJson, actualTopoJson);
	}

	private boolean deleteFile(Path filePath) {
		try {
			return Files.deleteIfExists(filePath);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;

	}

}
