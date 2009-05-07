package ru.onlytime.ssh.deploy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Locale;

import javax.enterprise.deploy.model.DeployableObject;
import javax.enterprise.deploy.shared.CommandType;
import javax.enterprise.deploy.shared.DConfigBeanVersionType;
import javax.enterprise.deploy.shared.ModuleType;
import javax.enterprise.deploy.spi.DeploymentConfiguration;
import javax.enterprise.deploy.spi.DeploymentManager;
import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.exceptions.DConfigBeanVersionUnsupportedException;
import javax.enterprise.deploy.spi.exceptions.InvalidModuleException;
import javax.enterprise.deploy.spi.exceptions.TargetException;
import javax.enterprise.deploy.spi.status.ProgressObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.onlytime.ssh.deploy.spi.DeploymentStatusImpl;
import ru.onlytime.ssh.deploy.spi.ProgressObjectImpl;
import ru.onlytime.ssh.deploy.spi.TargetImpl;
import ru.onlytime.ssh.deploy.spi.TargetModuleIDImpl;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class DeploymentManagerImpl implements DeploymentManager {

	private final static Log log = LogFactory.getLog(DeploymentManagerImpl.class);

	private String serverName;
	private Session session;

	public DeploymentManagerImpl(String host, Integer port, String path, String login, String password) {

		if (host == null || host.trim().length() == 0 || port == null || path == null | path.trim().length() == 0 || login == null || login.trim().length() == 0) {
			throw new IllegalArgumentException("wrong parameters specified");
		}

		serverName = host + ":" + port + ":" + path;
		JSch sch = new JSch();
		try {
			session = sch.getSession(login, host, port);
			session.setPassword(password);
			session.connect();
		} catch (JSchException e) {
			log.error("cannot obtain session for login: " + login + " host: " + host + " port: " + port, e);
			throw new RuntimeException("cannot obtain session for login: " + login + " host: " + host + " port: " + port, e);
		}

	}

	public ProgressObject distribute(Target[] targets, File archive, File plan) throws IllegalStateException {
		if (!archive.getName().endsWith(".jar")) {
			throw new IllegalArgumentException("Only .jar archive is supported");
		}
		InputStream archiveStream;
		try {
			archiveStream = new FileInputStream(archive);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		return distribute(targets, ModuleType.EJB, archiveStream, null);
	}

	public ProgressObject distribute(Target[] targets, ModuleType type, InputStream archive, InputStream plan) throws IllegalStateException {

		if (!type.equals(ModuleType.EJB)) {
			throw new IllegalStateException("Only JAR is supported");
		}

		String tag = "/jsr88-" + String.valueOf(System.currentTimeMillis());
		TargetModuleIDImpl moduleID = new TargetModuleIDImpl(targets[0], tag);

		return deploy(moduleID, archive);
	}

	private ProgressObject deploy(TargetModuleID module, InputStream archive) {
		ProgressObject result = null;

		String command = "scp -f ";
		try {
			Channel channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);
			return result;
		} catch (Exception e) {
			log.error("cannot obtain modules", e);
			DeploymentStatusImpl status = new DeploymentStatusImpl("cannot process request", CommandType.DISTRIBUTE, true);
			result = new ProgressObjectImpl(status, null);
			return result;
		}

		// PutMethod deploy = new PutMethod(url.toString() + "deploy");
		// deploy.setRequestEntity(new InputStreamRequestEntity(archive));
		// deploy.setQueryString(new NameValuePair[] { new NameValuePair("tag",
		// module.getModuleID()), new NameValuePair("path", module.getWebURL()),
		// new NameValuePair("update", "true") });
		// int code = client.executeMethod(deploy);
		// if (code != 200) {
		// DeploymentStatusImpl status = new
		// DeploymentStatusImpl("wrong response code: " + code,
		// CommandType.DISTRIBUTE, true);
		// result = new ProgressObjectImpl(status, null);
		// return result;
		// }
		//
		// String response = deploy.getResponseBodyAsString();
		//
		// if (response.startsWith("FAIL")) {
		// DeploymentStatusImpl status = new DeploymentStatusImpl(response,
		// CommandType.DISTRIBUTE, true);
		// result = new ProgressObjectImpl(status, module);
		// return result;
		// }
		//
		// // stop after deploy. tomcat feature
		//
		// result = stop(new TargetModuleID[] { module });
		// if (result.getDeploymentStatus().isCompleted()) {
		// DeploymentStatusImpl status = new DeploymentStatusImpl(response,
		// CommandType.DISTRIBUTE, false);
		// result = new ProgressObjectImpl(status, module);
		// } else {
		// DeploymentStatusImpl status = new
		// DeploymentStatusImpl(result.getDeploymentStatus().getMessage(),
		// CommandType.DISTRIBUTE, true);
		// result = new ProgressObjectImpl(status, module);
		// }
		//
		// return result;
		//

	}

	public TargetModuleID[] getAvailableModules(ModuleType arg0, Target[] arg1) throws TargetException, IllegalStateException {
		// TODO Auto-generated method stub
		return null;
	}

	public TargetModuleID[] getNonRunningModules(ModuleType arg0, Target[] arg1) throws TargetException, IllegalStateException {
		// TODO Auto-generated method stub
		return null;
	}

	public TargetModuleID[] getRunningModules(ModuleType arg0, Target[] arg1) throws TargetException, IllegalStateException {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isRedeploySupported() {
		return true;
	}

	public ProgressObject redeploy(TargetModuleID[] arg0, File arg1, File arg2) throws UnsupportedOperationException, IllegalStateException {
		// TODO Auto-generated method stub
		return null;
	}

	public ProgressObject redeploy(TargetModuleID[] arg0, InputStream arg1, InputStream arg2) throws UnsupportedOperationException, IllegalStateException {
		// TODO Auto-generated method stub
		return null;
	}

	public ProgressObject start(TargetModuleID[] arg0) throws IllegalStateException {
		// TODO Auto-generated method stub
		return null;
	}

	public ProgressObject stop(TargetModuleID[] arg0) throws IllegalStateException {
		// TODO Auto-generated method stub
		return null;
	}

	public ProgressObject undeploy(TargetModuleID[] arg0) throws IllegalStateException {
		// TODO Auto-generated method stub
		return null;
	}

	public DeploymentConfiguration createConfiguration(DeployableObject arg0) throws InvalidModuleException {
		throw new UnsupportedOperationException();
	}

	public ProgressObject distribute(Target[] arg0, InputStream arg1, InputStream arg2) throws IllegalStateException {
		throw new UnsupportedOperationException();
	}

	public Locale getCurrentLocale() {
		throw new UnsupportedOperationException();
	}

	public DConfigBeanVersionType getDConfigBeanVersion() {
		throw new UnsupportedOperationException();
	}

	public Locale getDefaultLocale() {
		throw new UnsupportedOperationException();
	}

	public Locale[] getSupportedLocales() {
		throw new UnsupportedOperationException();
	}

	public boolean isDConfigBeanVersionSupported(DConfigBeanVersionType arg0) {
		return false;
	}

	public boolean isLocaleSupported(Locale arg0) {
		return false;
	}

	public void setDConfigBeanVersion(DConfigBeanVersionType arg0) throws DConfigBeanVersionUnsupportedException {
		throw new UnsupportedOperationException();
	}

	public void setLocale(Locale arg0) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	public Target[] getTargets() throws IllegalStateException {
		return new Target[] { new TargetImpl(serverName) };
	}

	public void release() {
		if (session != null) {
			session.disconnect();
		}
	}

}
