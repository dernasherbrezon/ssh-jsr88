package ru.onlytime.ssh.deploy.spi;

import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;

public class TargetModuleIDImpl implements TargetModuleID
{
	private Target target;
	private String moduleID;
	private boolean isRunning = false;

	public TargetModuleIDImpl(Target target, String moduleID)
	{
		this.target = target;
		this.moduleID = moduleID;
	}

	public TargetModuleID[] getChildTargetModuleID()
	{
		return null;
	}

	public String getModuleID()
	{
		return moduleID;
	}

	public TargetModuleID getParentTargetModuleID()
	{
		return null;
	}

	public Target getTarget()
	{
		return target;
	}

	public String getWebURL()
	{
		return moduleID;
	}

	public boolean isRunning()
	{
		return isRunning;
	}

	public void setRunning(boolean isRunning)
	{
		this.isRunning = isRunning;
	}

	@Override
	public String toString()
	{
		return getTarget().getName() + "-" + getModuleID();
	}

}
