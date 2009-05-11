package ru.onlytime.ssh.deploy.spi;

import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;

public class TargetModuleIDImpl implements TargetModuleID
{
	private Target target;
	private String moduleID;
	private Integer pid;

	public TargetModuleIDImpl() {
	}
	
	public TargetModuleIDImpl(TargetModuleID copy) {
		this.target = copy.getTarget();
		this.moduleID = copy.getModuleID();
	}
	
	public Integer getPid() {
		return pid;
	}

	public void setPid(Integer pid) {
		this.pid = pid;
	}

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
		return null;
	}

	@Override
	public String toString()
	{
		return getTarget().getName() + "-" + getModuleID();
	}

}
