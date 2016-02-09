package interactors;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class XmlRule {
	public static String toXml(Object object) {
		String result = xstream(object);
		return result;
	}
	
	private static String xstream(Object object) {
        XStream xstream = getXStream();
		return xstream.toXML(object);
	}

	private static XStream getXStream() {
		XStream xstream = new XStream(new DomDriver());
		return xstream;
	}
	
	public static Object toObject(String xml){
		XStream xstream = getXStream();
		return xstream.fromXML(xml);
	}
}
