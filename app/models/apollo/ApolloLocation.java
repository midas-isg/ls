package models.apollo;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import edu.pitt.apollo.types.v3_0_0.Location;

@XmlRootElement(name="Location", namespace="types")
@JsonInclude(Include.NON_NULL)
public class ApolloLocation extends Location implements Apollo {

 }
