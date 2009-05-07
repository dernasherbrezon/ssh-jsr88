package ru.onlytime.ssh.deploy;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.enterprise.deploy.spi.DeploymentManager;
import javax.enterprise.deploy.spi.exceptions.DeploymentManagerCreationException;
import javax.enterprise.deploy.spi.factories.DeploymentFactory;

public class DeploymentFactoryImpl implements DeploymentFactory {

	private final static String URI_PREFIX = "deployer:ru.onlytime.ssh:";
	private final static Pattern appPattern = Pattern.compile(URI_PREFIX + "(\\w+):(\\d+):(\\w+\\\\)");

	public DeploymentManager getDeploymentManager(String uri, String login, String password) throws DeploymentManagerCreationException {
		
		Matcher m = appPattern.matcher(uri);
		if (!m.find()) {
			throw new DeploymentManagerCreationException("wrong uri specified. valid uri: \"deployer:ru.onlytime.ssh:localhost:22:/usr/local/\"");
		}
		
		String host = m.group(1);
		String portStr = m.group(2);
		String path = m.group(3);
		
		return null;
	}

	public DeploymentManager getDisconnectedDeploymentManager(String uri) throws DeploymentManagerCreationException {
		throw new UnsupportedOperationException("not implemented");
	}

	public String getDisplayName() {
		return "SSH remote jsr88 deployer";
	}

	public String getProductVersion() {
		return "1.0.0";
	}

	public boolean handlesURI(String uri) {
		Matcher m = appPattern.matcher(uri);
		if (m.find()) {
			return true;
		}
		return false;
	}

}
