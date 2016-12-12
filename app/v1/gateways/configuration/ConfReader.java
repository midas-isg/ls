package v1.gateways.configuration;

import play.Configuration;
import play.Play;

public class ConfReader {
	private static Configuration conf = null; 
	
	public String readString(String key){
		return getConf().getString(key);
	}

	private Configuration getConf() {
		if (conf == null)
			conf = Play.application().configuration();
		return conf;
	}
}
