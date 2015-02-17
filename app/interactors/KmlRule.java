package interactors;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;

import play.Play;
import dao.entities.Data;
import dao.entities.Location;

public class KmlRule {
	public static String asKml(Location location) {
		Data data = location.getData();
		String kml = data.getKml();
		if (kml != null && !kml.isEmpty())
			return kml;
		Long gid = location.getGid();
		String kmlGeo = GeometryRule.readAsKml(gid);
		String fileName = "template.kml";
		String formatText = getStringFromFile(fileName);
		String text = String.format(formatText, gid, 
				data.getName(), data.getDescription(), kmlGeo);
		return text;
	}

	private static String getStringFromFile(String fileName) {
		URL url = Play.application().classloader().getResource(fileName);
		String formatText = "";
		try {
			InputStream is = url.openStream();
			formatText = getStringFromStream(is);
		} catch (IOException e) {
			String message = "Error during opening file " + fileName
					+ ". Check if configuration is correct.";
			throw new RuntimeException(message, e);
		}
		return formatText;
	}
	
	private static String getStringFromStream(InputStream is) {
		String text = "";
		try (Scanner s = new Scanner(is, "UTF-8")){
			s.useDelimiter("\\A");
			text = s.hasNext() ? s.next() : "";
		}
		return text;
	}
}
