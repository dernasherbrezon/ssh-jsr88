package ru.onlytime.ssh.deploy.spi;

import javax.enterprise.deploy.shared.ActionType;
import javax.enterprise.deploy.shared.CommandType;
import javax.enterprise.deploy.shared.StateType;

public class DeploymentStatusImpl implements javax.enterprise.deploy.spi.status.DeploymentStatus
{
	private CommandType type;
	private String message;
	private boolean isFailed = false;

	public DeploymentStatusImpl(String message, CommandType type, boolean isFailed)
	{
		this.message = message;
		this.type = type;
		this.isFailed = isFailed;
	}

	public ActionType getAction()
	{
		return ActionType.EXECUTE; //stop and cancel not supported
	}

	public CommandType getCommand()
	{
		return type;
	}

	public String getMessage()
	{
		return message; //only one message because of sync mode
	}

	public StateType getState()
	{
		return StateType.COMPLETED; //do tomcat operations in sync mode.
	}

	public boolean isCompleted()
	{
		return !isFailed;
	}

	public boolean isFailed()
	{
		return isFailed;
	}

	public boolean isRunning()
	{
		return false;
	}

}
