package ru.onlytime.ssh.deploy.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import ru.onlytime.ssh.deploy.PSAuxParser;

public class PSAuxParserTest {

	private static final String sampleOutput = "root      8419  0.1  0.6 771160 12916 ?        Ssl  17:44   0:00 /usr/lib/jvm/sun-jdk-1.6/bin/java -jar /home/dernasherbrezon/jsr88-1242022916529.jar";

	@Test
	public void testGetCommand() {
		String result = PSAuxParser.getCommand("/home/dernasherbrezon/", sampleOutput);
		assertNotNull(result);
		assertEquals("jsr88-1242022916529.jar", result);
	}

	@Test
	public void testGetPID() {
		Integer result = PSAuxParser.getPID(sampleOutput);
		assertNotNull(result);
		assertEquals(8419, result.intValue());
	}

}
