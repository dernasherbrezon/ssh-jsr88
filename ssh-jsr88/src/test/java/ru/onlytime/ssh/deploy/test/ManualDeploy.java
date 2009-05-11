package ru.onlytime.ssh.deploy.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.enterprise.deploy.shared.ModuleType;
import javax.enterprise.deploy.spi.DeploymentManager;
import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.status.ProgressObject;

import org.junit.Before;
import org.junit.Test;

import ru.onlytime.ssh.deploy.DeploymentFactoryImpl;

public class ManualDeploy {

	private DeploymentManager manager;

	private final static String URI_PREFIX = "deployer:ru.onlytime.ssh:";
	private final static Pattern appPattern = Pattern.compile(URI_PREFIX + "([\\w\\.]+):(\\d+):([\\w/]+)");

	@Test
	public void testPattern() {
		Matcher m = appPattern.matcher("deployer:ru.onlytime.ssh:192.168.1.2:22:/home/dernasherbrezon/");
		assertTrue(m.find());
		assertEquals("192.168.1.2", m.group(1));
		assertEquals("22", m.group(2));
		assertEquals("/home/dernasherbrezon/", m.group(3));
	}
	
	@Test
	public void testServerDeploy() throws Exception {
		manager = new DeploymentFactoryImpl().getDeploymentManager("deployer:ru.onlytime.ssh:192.168.1.2:22:/home/dernasherbrezon/", "root", "1");
		Target[] availableTargets = manager.getTargets();

		ProgressObject result = manager.distribute(availableTargets, ModuleType.EJB, new FileInputStream("sample-server-1.0.0.jar"), null);
		assertNotNull(result);
		assertTrue(result.getDeploymentStatus().isCompleted());
//
		TargetModuleID[] availableModules = manager.getAvailableModules(ModuleType.EJB, availableTargets);
		
		ProgressObject res = manager.start(availableModules);
		assertTrue(res.getDeploymentStatus().isCompleted());
		System.out.println(res.getDeploymentStatus().getMessage());

		Thread.sleep(5000);
		
		res = manager.stop(availableModules);
		assertTrue(res.getDeploymentStatus().isCompleted());
		System.out.println(res.getDeploymentStatus().getMessage());

		Thread.sleep(2000);
		
		res = manager.redeploy(availableModules, new File("sample-jar-1.0.02.jar"), new File("test.properties"));
		System.out.println(res.getDeploymentStatus().getMessage());
		assertTrue(res.getDeploymentStatus().isCompleted());
		
//		res = manager.undeploy(availableModules);
//		System.out.println(res.getDeploymentStatus().getMessage());
//		assertTrue(res.getDeploymentStatus().isCompleted());
		
//		manager.release();
	}

	@Test
	public void testDeploy() throws Exception {
		// manager = new
		// DeploymentFactoryImpl().getDeploymentManager("deployer:ru.onlytime.ssh:192.168.1.2:22:/home/dernasherbrezon/",
		// "root", "1");
		// Target[] availableTargets = manager.getTargets();
		// assertNotNull(availableTargets);
		// assertEquals(1, availableTargets.length);
		// assertEquals("192.168.1.2:22:/home/dernasherbrezon/",
		// availableTargets[0].getName());

		// ProgressObject result = manager.distribute(availableTargets,
		// ModuleType.EJB, new FileInputStream("sample-jar-1.0.0.jar"), null);
		// assertNotNull(result);
		// assertTrue(result.getDeploymentStatus().isCompleted());

		// TargetModuleID[] modules =
		// manager.getAvailableModules(ModuleType.EJB, availableTargets);
		// assertNotNull(modules);
		// assertEquals(1, modules.length);
		// System.out.println(modules[0].getModuleID());

		// ProgressObject obj = manager.start(modules);
		// System.out.println(obj.getDeploymentStatus().getMessage());
		//
		// manager.release();
	}

	@Before
	public void start() throws Exception {
	}

}
