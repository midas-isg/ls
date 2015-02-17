package interactors;

import java.io.ByteArrayOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

public class XmlRule {
	public static String toXml(Object object) throws Exception{
		JAXBContext ctx = JAXBContext.newInstance(object.getClass());
		Marshaller jaxbMarshaller = ctx.createMarshaller();
		 
		jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
 
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		jaxbMarshaller.marshal(object, baos);
		String result = baos.toString();
		return result;
	}
}
