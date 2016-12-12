package v1.interactors;

import v1.gateways.configuration.ConfReader;

public class ConfRule {
	private ConfReader reader = null;

	public ConfRule(ConfReader reader) {
		this.reader = reader;
	}

	public String readString(String key) {
		return reader.readString(key);
	}
}
