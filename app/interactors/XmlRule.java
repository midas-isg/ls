package interactors;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class XmlRule {
	public static String toXml(Object object) throws Exception{
		String result = xstream(object);
		return result;
	}
	
	private static String xstream(Object object) {
        XStream xstream = new XStream(new DomDriver());
		return xstream.toXML(object);
	}
}
