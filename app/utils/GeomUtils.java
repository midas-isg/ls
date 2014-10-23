package utils;

import java.io.IOException;
import java.io.StringWriter;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTWriter;

public class GeomUtils {

	/***
	 * 
	 * @param geom
	 * @return
	 */
	public static String GeomToWKT(Geometry geom) {
		
        StringWriter writer = new StringWriter();
        WKTWriter wktWriter = new WKTWriter(2);
        
        try {
            wktWriter.write( geom, writer );
        } catch (IOException e) {            
        }
        
        String wkt = writer.toString();
                
        return wkt;
		
	}
	
	/***
	 * 
	 * Convert a WKT to <i>SVG Path Definition</i>
	 * 
	 *	MULTIPOLYGON(((
	 *   	585508.753489015 4509691.267208,
	 *   	586826.357059039 4508984.18775711,
	 *   	586726.826681175 4508315.279346,
	 *   	586679.913103019 4508101.72328801,
	 *   	586517.254941174 4508132.00302681,
	 *   	585074.213209971 4508672.63824408,
	 *   	585159.349698787 4509010.01258964,
	 *   	585508.753489015 4509691.267208)))
	 * 
	 * M 585508.75348901481 -4509691.2672080006 
	 * L 586826.35705903941 -4508984.1877571139 
	 *   586726.8266811748 -4508315.2793459995 
	 *   586679.91310301947 -4508101.7232880052 
	 *   586517.25494117441 -4508132.0030268123 
	 *   585074.21320997074 -4508672.6382440841 
	 *   585159.34969878721 -4509010.0125896409 Z
	 *   
	 * @param WKT
	 * @return
	 * 
	 */
	public static String WKTToSVGPath(String wkt) {
		
		StringBuffer svg = new StringBuffer();
		
		svg.append("M ");
		
		wkt = wkt.toUpperCase();
		
		wkt = wkt.replaceAll("MULTIPOLYGON", "");
		wkt = wkt.replace("(", "");
		wkt = wkt.replace(")", "");
		
	
		String[] tokens = wkt.split(",");  
		
		String[] coordinates = tokens[0].trim().split(" ");

		svg.append(coordinates[0]);
		svg.append(" -");
		svg.append(coordinates[1]);
		svg.append(" L ");

		for(int i = 1; i < tokens.length; i++) {
			
			coordinates = tokens[i].trim().split(" ");
			
			svg.append(coordinates[0]);
			svg.append(" -");
			svg.append(coordinates[1]);
			svg.append(" ");
			
		}
		
		svg.append("Z");
		
		return svg.toString();
	}
	
	/***
	 * 
	 * @param geom
	 * @return
	 */
	public static String GeomToSVGPath(Geometry geom) {
		return WKTToSVGPath(GeomToWKT(geom));
	}
}
