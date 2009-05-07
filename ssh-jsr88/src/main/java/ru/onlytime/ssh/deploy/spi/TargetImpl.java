package ru.onlytime.ssh.deploy.spi;

import javax.enterprise.deploy.spi.Target;

public class TargetImpl implements Target
{
	private String name;
	
	public TargetImpl(String name)
	{
		this.name = name;
	}

	public String getDescription()
	{
		return "tomcat instance";
	}

	public String getName()
	{
		return name;
	}

}
