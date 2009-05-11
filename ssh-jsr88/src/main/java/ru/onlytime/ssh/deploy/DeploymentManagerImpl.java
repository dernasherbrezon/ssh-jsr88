package ru.onlytime.ssh.deploy;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;

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
import ru.onlytime.ssh.deploy.spi.ProgressObjectMultipleModules;
import ru.onlytime.ssh.deploy.spi.TargetImpl;
import ru.onlytime.ssh.deploy.spi.TargetModuleIDImpl;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public class DeploymentManagerImpl implements DeploymentManager {

	private final static Log log = LogFactory.getLog(DeploymentManagerImpl.class);

	private String serverName;
	private Session session;
	private String path;

	// TODO validate server. check ps command at least
	// TODO create correct unit file permissions for .jar and .sh files. Only login can run and delete them.
	public DeploymentManagerImpl(String host, Integer port, String path, String login, String password) {

		if (host == null || host.trim().length() == 0 || port == null || path == null || path.trim().length() == 0 || login == null || login.trim().length() == 0) {
			throw new IllegalArgumentException("wrong parameters specified");
		}

		if (path.endsWith("/")) {
			this.path = path;
		} else {
			this.path = path + "/";
		}
		serverName = host + ":" + port + ":" + path;
		JSch sch = new JSch();
		try {
			session = sch.getSession(login, host, port);
			session.setPassword(password);
			session.setConfig("StrictHostKeyChecking", "no");
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
		InputStream planStream = null;
		if (plan != null) {
			try {
				planStream = new FileInputStream(plan);
			} catch (FileNotFoundException e) {
				log.warn("cannot load specified file as properties. use default: java -jar archive.jar", e);
			}
		}

		return distribute(targets, ModuleType.EJB, archiveStream, planStream);
	}

	public ProgressObject distribute(Target[] targets, ModuleType type, InputStream archive, InputStream plan) throws IllegalStateException {

		if (!type.equals(ModuleType.EJB)) {
			throw new IllegalStateException("Only JAR is supported");
		}

		String tag = "jsr88-" + String.valueOf(System.currentTimeMillis());
		TargetModuleIDImpl moduleID = new TargetModuleIDImpl(targets[0], tag);

		Properties props = null;
		if (plan != null) {
			try {
				props = new Properties();
				props.load(plan);
			} catch (IOException e) {
				log.warn("cannot load specified properties. use default: java -jar archive.jar", e);
			}
		}

		return deploy(moduleID, archive, props);
	}

	private ProgressObject deploy(TargetModuleID module, InputStream archive, Properties props) {
		ProgressObject result = null;

		try {
			String command = "scp -p -t " + path;

			Channel channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);

			OutputStream out = channel.getOutputStream();
			InputStream in = channel.getInputStream();

			channel.connect();

			result = checkResponse(in, module);
			if (result != null) {
				return result;
			}

			command = "C0644 " + archive.available() + " ";
			command += module.getModuleID() + ".jar";
			command += "\n";

			out.write(command.getBytes());
			out.flush();

			result = checkResponse(in, module);
			if (result != null) {
				return result;
			}

			sendFile(out, archive);

			result = checkResponse(in, module);
			if (result != null) {
				return result;
			}

			String startScript = obtainStartScript(props, module);
			byte[] bytesToWrite = startScript.getBytes("UTF-8");

			command = "C0647 " + bytesToWrite.length + " ";
			command += module.getModuleID() + ".sh";
			command += "\n";

			out.write(command.getBytes());
			out.flush();

			result = checkResponse(in, module);
			if (result != null) {
				return result;
			}

			for (int i = 0; i < bytesToWrite.length; i++) {
				out.write(bytesToWrite[i]);
			}
			out.write(0);
			out.flush();

			result = checkResponse(in, module);
			if (result != null) {
				return result;
			}

			out.close();

			channel.disconnect();

			DeploymentStatusImpl status = new DeploymentStatusImpl("success", CommandType.DISTRIBUTE, false);
			result = new ProgressObjectImpl(status, module);
			return result;
		} catch (Exception e) {
			log.error("cannot obtain modules", e);
			DeploymentStatusImpl status = new DeploymentStatusImpl("cannot process request", CommandType.DISTRIBUTE, true);
			result = new ProgressObjectImpl(status, module);
			return result;
		}
	}

	private String obtainStartScript(Properties props, TargetModuleID module) {
		if (module == null) {
			throw new IllegalArgumentException("module cannot be null");
		}
		
		if (props == null) {
			return "java -jar " + path + module.getModuleID() + ".jar\n";
		}

		String javaOpts = props.getProperty("JAVA_OPTS");
		if (javaOpts == null) {
			javaOpts = "-jar";
		} else {
			if (javaOpts.contains("-jar")) {
				log.warn("no need to specify -jar in JAVA_OPTS.");
				int jarIndex = javaOpts.indexOf("-jar");
				if (jarIndex + "-jar".length() != javaOpts.trim().length()) { //if jar not in the end. cut first then paste at the end
					javaOpts = javaOpts.substring(0, jarIndex) + javaOpts.substring(jarIndex + "-jar".length(), javaOpts.length());
					javaOpts += " -jar";
				}
			} else {
				javaOpts += " -jar";
			}
		}

		String jarOpts = props.getProperty("JAR_OPTS");

		String result = "java " + javaOpts.trim() + " " + path + module.getModuleID() + ".jar";
		if (jarOpts != null) {
			result += " " + jarOpts;
		}
		result += "\n";
		return result;
	}

	private static ProgressObject checkResponse(InputStream in, TargetModuleID module, CommandType type) throws Exception {
		ProgressObject result = null;
		String response = getResponse(in);
		if (response != null) {
			log.debug("wrong response while distribute. resp: " + response);
			DeploymentStatusImpl status = new DeploymentStatusImpl(response, type, true);
			result = new ProgressObjectImpl(status, module);
		}
		return result;
	}

	private static ProgressObject checkResponse(InputStream in, TargetModuleID module) throws Exception {
		return checkResponse(in, module, CommandType.DISTRIBUTE);
	}

	private static String getResponse(InputStream in) throws IOException {
		int b = in.read();
		// b may be 0 for success,
		// 1 for error,
		// 2 for fatal error,
		if (b == 0 || b == -1) {
			return null;
		}

		if (b == 1 || b == 2) {
			StringBuffer sb = new StringBuffer();
			int c;
			do {
				c = in.read();
				sb.append((char) c);
			} while (c != '\n');
			return sb.toString();
		}
		return null;
	}

	private static void sendFile(OutputStream out, InputStream contents) throws IOException {
		// send a content of lfile
		int curByte = -1;
		while ((curByte = contents.read()) != -1) {
			out.write(curByte);
		}
		contents.close();
		// send '\0'
		out.write(0);
		out.flush();
	}

	@SuppressWarnings("unchecked")
	public TargetModuleID[] getAvailableModules(ModuleType type, Target[] targets) throws TargetException, IllegalStateException {
		if (!type.equals(ModuleType.EJB)) {
			return new TargetModuleID[0];
		}
		if (targets == null || targets.length == 0) {
			throw new TargetException("targets must not be null");
		}
		Channel channel = null;
		try {
			channel = session.openChannel("sftp");
			channel.connect();

			List<TargetModuleIDImpl> result = new LinkedList<TargetModuleIDImpl>();

			Vector res = ((ChannelSftp) channel).ls(path + "jsr88-*.jar");
			for (int i = 0; i < res.size(); i++) {

				Object obj = res.elementAt(i);
				if (obj instanceof com.jcraft.jsch.ChannelSftp.LsEntry) {
					String fileName = ((com.jcraft.jsch.ChannelSftp.LsEntry) obj).getFilename();
					String moduleId = fileName.substring(0, fileName.lastIndexOf("."));
					TargetModuleIDImpl curModule = new TargetModuleIDImpl(targets[0], moduleId);
					result.add(curModule);
				}
			}
			updateStatuses(result);

			return result.toArray(new TargetModuleID[0]);

		} catch (Exception e) {
			log.debug("cannot handle ls request.", e);
			return new TargetModuleID[0];
		} finally {
			if (channel != null) {
				channel.disconnect();
			}
		}

	}

	public TargetModuleID[] getNonRunningModules(ModuleType type, Target[] targets) throws TargetException, IllegalStateException {
		if (!type.equals(ModuleType.EJB)) {
			return new TargetModuleID[0];
		}

		TargetModuleID[] allModules = getAvailableModules(type, targets);
		List<TargetModuleID> result = new LinkedList<TargetModuleID>();
		for (TargetModuleID curModule : allModules) {
			if (((TargetModuleIDImpl) curModule).getPid() == null) {
				result.add(curModule);
			}
		}
		return result.toArray(new TargetModuleID[0]);
	}

	public TargetModuleID[] getRunningModules(ModuleType type, Target[] targets) throws TargetException, IllegalStateException {
		if (!type.equals(ModuleType.EJB)) {
			return new TargetModuleID[0];
		}

		TargetModuleID[] allModules = getAvailableModules(type, targets);
		List<TargetModuleID> result = new LinkedList<TargetModuleID>();
		for (TargetModuleID curModule : allModules) {
			if (((TargetModuleIDImpl) curModule).getPid() != null) {
				result.add(curModule);
			}
		}
		return result.toArray(new TargetModuleID[0]);
	}

	public boolean isRedeploySupported() {
		return true;
	}

	public ProgressObject redeploy(TargetModuleID[] modules, File archive, File plan) throws UnsupportedOperationException, IllegalStateException {
		if (!archive.getName().endsWith(".jar")) {
			throw new IllegalArgumentException("Only .jar archive is supported");
		}
		InputStream archiveStream;
		InputStream planStream = null;
		try {
			archiveStream = new FileInputStream(archive);
			if (plan != null) {
				planStream = new FileInputStream(plan);
			}
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		return redeploy(modules, archiveStream, planStream);
	}

	public ProgressObject redeploy(TargetModuleID[] modules, InputStream archive, InputStream plan) throws UnsupportedOperationException, IllegalStateException {
		if (modules == null || modules.length == 0) {
			throw new IllegalArgumentException("modules cannot be null");
		}

		if (modules.length > 1) {
			log.info("multiple modules for redeploy specified. only first will be processed");
		}

		ProgressObject result = null;

		List<TargetModuleIDImpl> internal = convertToInternal(modules);
		updateStatuses(internal);

		if (internal.get(0).getPid() != null) {
			log.debug("cannot redeploy started module. module id: " + internal.get(0).getModuleID());
			DeploymentStatusImpl status = new DeploymentStatusImpl("cannot redeploy started module. stop first", CommandType.REDEPLOY, true);
			result = new ProgressObjectImpl(status, modules[0]);
			return result;
		}

		try {
			String command = "rm " + path + modules[0].getModuleID() + ".* & scp -p -t " + path;

			Channel channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);

			OutputStream out = channel.getOutputStream();
			InputStream in = channel.getInputStream();

			channel.connect();

			result = checkResponse(in, modules[0], CommandType.REDEPLOY);
			if (result != null) {
				return result;
			}

			command = "C0644 " + archive.available() + " ";
			command += modules[0].getModuleID() + ".jar";
			command += "\n";

			out.write(command.getBytes());
			out.flush();

			result = checkResponse(in, modules[0], CommandType.REDEPLOY);
			if (result != null) {
				return result;
			}

			sendFile(out, archive);

			result = checkResponse(in, modules[0], CommandType.REDEPLOY);
			if (result != null) {
				return result;
			}

			Properties props = null;
			if (plan != null) {
				props = new Properties();
				props.load(plan);
			}

			String startScript = obtainStartScript(props, modules[0]);
			byte[] bytesToWrite = startScript.getBytes("UTF-8");

			command = "C0647 " + bytesToWrite.length + " ";
			command += modules[0].getModuleID() + ".sh";
			command += "\n";

			out.write(command.getBytes());
			out.flush();

			result = checkResponse(in, modules[0]);
			if (result != null) {
				return result;
			}

			for (int i = 0; i < bytesToWrite.length; i++) {
				out.write(bytesToWrite[i]);
			}
			out.write(0);
			out.flush();

			out.close();

			channel.disconnect();

			DeploymentStatusImpl status = new DeploymentStatusImpl("success", CommandType.REDEPLOY, false);
			result = new ProgressObjectImpl(status, modules[0]);
			return result;
		} catch (Exception e) {
			log.error("cannot obtain modules", e);
			DeploymentStatusImpl status = new DeploymentStatusImpl("cannot process request", CommandType.REDEPLOY, true);
			result = new ProgressObjectImpl(status, modules[0]);
			return result;
		}
	}

	public ProgressObject start(TargetModuleID[] modules) throws IllegalStateException {
		if (modules == null || modules.length == 0) {
			throw new IllegalArgumentException("modules cannot be null");
		}

		ProgressObjectMultipleModules result = new ProgressObjectMultipleModules();

		List<TargetModuleIDImpl> internal = convertToInternal(modules);
		updateStatuses(internal);

		for (TargetModuleIDImpl curModule : internal) {
			if (curModule.getPid() != null) { // already started
				log.debug("module: " + curModule.getModuleID() + " already started");
				DeploymentStatusImpl status = new DeploymentStatusImpl("already started", CommandType.START, false);
				result.addModuleProcessResult(status, curModule);
				continue;
			}
			Channel channel = null;
			try {
				channel = session.openChannel("exec");
				((ChannelExec) channel).setCommand(path + curModule.getModuleID() + ".sh");

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				((ChannelExec) channel).setErrStream(baos);

				channel.connect();

				if (baos.size() > 0) {
					String error = new String(baos.toByteArray(), "UTF-8");
					DeploymentStatusImpl status = new DeploymentStatusImpl(error, CommandType.START, true);
					result.addModuleProcessResult(status, curModule);
				} else {
					DeploymentStatusImpl status = new DeploymentStatusImpl("success", CommandType.START, false);
					result.addModuleProcessResult(status, curModule);
				}

			} catch (Exception e) {
				log.error("cannot start module", e);
				DeploymentStatusImpl status = new DeploymentStatusImpl(e.getMessage(), CommandType.START, true);
				result.addModuleProcessResult(status, curModule);
			} finally {
				if (channel != null) {
					channel.disconnect();
				}
			}
		}
		return result;
	}

	public ProgressObject stop(TargetModuleID[] modules) throws IllegalStateException {
		List<TargetModuleIDImpl> internal = convertToInternal(modules);
		updateStatuses(internal);

		ProgressObjectMultipleModules result = new ProgressObjectMultipleModules();

		Channel channel = null;
		for (TargetModuleIDImpl curModule : internal) {
			if (curModule.getPid() == null) {
				log.debug("module " + curModule.getModuleID() + " not started");
				DeploymentStatusImpl status = new DeploymentStatusImpl("not started", CommandType.STOP, false);
				result.addModuleProcessResult(status, curModule);
				continue;
			}

			try {
				channel = session.openChannel("exec");
				((ChannelExec) channel).setCommand("kill " + curModule.getPid());

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				((ChannelExec) channel).setErrStream(baos);

				channel.connect();

				if (baos.size() > 0) {
					String error = new String(baos.toByteArray(), "UTF-8");
					DeploymentStatusImpl status = new DeploymentStatusImpl(error, CommandType.START, true);
					result.addModuleProcessResult(status, curModule);
				} else {
					DeploymentStatusImpl status = new DeploymentStatusImpl("success", CommandType.START, false);
					result.addModuleProcessResult(status, curModule);
				}

			} catch (Exception e) {
				log.debug("cannot stop module " + curModule.getModuleID(), e);
				DeploymentStatusImpl status = new DeploymentStatusImpl(e.getMessage(), CommandType.STOP, true);
				result.addModuleProcessResult(status, curModule);
			} finally {
				if (channel != null) {
					channel.disconnect();
				}
			}
		}
		return result;
	}

	public ProgressObject undeploy(TargetModuleID[] modules) throws IllegalStateException {

		List<TargetModuleIDImpl> internal = convertToInternal(modules);
		updateStatuses(internal);

		ProgressObjectMultipleModules result = null;
		Channel channel;
		try {
			channel = session.openChannel("sftp");
			channel.connect();
		} catch (JSchException e) {
			log.error("connectivity problem", e);
			throw new RuntimeException(e);
		}

		result = new ProgressObjectMultipleModules();

		for (TargetModuleIDImpl curModule : internal) {
			if (curModule.getPid() != null) {
				log.debug("module: " + curModule.getModuleID() + " not stopped");
				DeploymentStatusImpl status = new DeploymentStatusImpl("not stopped", CommandType.UNDEPLOY, true);
				result.addModuleProcessResult(status, curModule);
				continue;
			}
			try {
				((ChannelSftp) channel).rm(path + curModule.getModuleID() + ".jar");
				DeploymentStatusImpl status = new DeploymentStatusImpl("success", CommandType.UNDEPLOY, false);
				result.addModuleProcessResult(status, curModule);
			} catch (SftpException e) {
				log.debug("cannot remove archive.", e);
				DeploymentStatusImpl status = new DeploymentStatusImpl(e.getMessage(), CommandType.UNDEPLOY, true);
				result.addModuleProcessResult(status, curModule);
			}
		}

		channel.disconnect();

		return result;
	}

	private static List<TargetModuleIDImpl> convertToInternal(TargetModuleID[] modules) {
		List<TargetModuleIDImpl> result = new LinkedList<TargetModuleIDImpl>();
		for (TargetModuleID curModule : modules) {
			TargetModuleIDImpl impl = new TargetModuleIDImpl(curModule);
			result.add(impl);
		}
		return result;
	}

	private void updateStatuses(List<TargetModuleIDImpl> modules) {
		Channel channel = null;
		try {
			channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand("ps aux | grep jsr88-");

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			((ChannelExec) channel).setErrStream(baos);
			InputStream output = ((ChannelExec) channel).getInputStream();
			channel.connect();

			if (baos.size() > 0) {
				String error = new String(baos.toByteArray(), "UTF-8");
				log.error("cannot get process information: " + error);
				return;
			}

			baos = new ByteArrayOutputStream();
			int b = -1;
			while ((b = output.read()) != -1) {
				baos.write(b);
			}

			String outputStr = new String(baos.toByteArray(), "UTF-8");
			String[] lines = outputStr.split("\n");

			for (String curLine : lines) {

				String moduleCommand = PSAuxParser.getCommand(path, curLine);
				if (moduleCommand == null) {
					continue;
				}

				if (!moduleCommand.contains(".jar")) {
					continue;
				}

				String moduleId = moduleCommand.substring(0, moduleCommand.indexOf(".jar"));
				TargetModuleIDImpl curModule = getModuleByID(modules, moduleId);
				if (curModule == null) {
					continue;
				}

				Integer pid = PSAuxParser.getPID(curLine);
				if (pid == null) {
					continue;
				}

				curModule.setPid(pid);
			}

		} catch (Exception e) {
			log.error("connectivity problem", e);
		} finally {
			if (channel != null) {
				channel.disconnect();
			}
		}
	}

	private static TargetModuleIDImpl getModuleByID(List<TargetModuleIDImpl> modules, String id) {
		for (TargetModuleIDImpl curModule : modules) {
			if (curModule.getModuleID().equals(id)) {
				return curModule;
			}
		}
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
