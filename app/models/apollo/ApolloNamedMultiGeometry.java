package models.apollo;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import edu.pitt.apollo.types.v3_0_0.NamedMultiGeometry;

@XmlRootElement(name="NamedMultiGeometry", namespace="types")
@JsonInclude(Include.NON_NULL)
public class ApolloNamedMultiGeometry 
extends NamedMultiGeometry 
implements Apollo  {

}
