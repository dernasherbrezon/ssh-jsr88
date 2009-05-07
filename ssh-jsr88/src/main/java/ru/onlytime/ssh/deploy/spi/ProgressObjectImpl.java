package ru.onlytime.ssh.deploy.spi;

import java.util.LinkedList;
import java.util.List;

import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.exceptions.OperationUnsupportedException;
import javax.enterprise.deploy.spi.status.ClientConfiguration;
import javax.enterprise.deploy.spi.status.DeploymentStatus;
import javax.enterprise.deploy.spi.status.ProgressEvent;
import javax.enterprise.deploy.spi.status.ProgressListener;
import javax.enterprise.deploy.spi.status.ProgressObject;

public class ProgressObjectImpl implements ProgressObject
{
	private List<ProgressListener> listeners = new LinkedList<ProgressListener>();
	private DeploymentStatus status;
	private TargetModuleID module;

	public ProgressObjectImpl(DeploymentStatus status, TargetModuleID module) //sync mode
	{
		this.status = status;
		this.module = module;
	}

	public DeploymentStatus getDeploymentStatus()
	{
		return status;
	}

	public TargetModuleID[] getResultTargetModuleIDs()
	{
		return new TargetModuleID[] { module };
	}

	public void addProgressListener(ProgressListener listener)
	{
		listeners.add(listener);
		listener.handleProgressEvent(new ProgressEvent(this, module, status));
	}

	public void removeProgressListener(ProgressListener listener)
	{
		listeners.remove(listener);
	}

	public void cancel() throws OperationUnsupportedException
	{
		throw new UnsupportedOperationException();
	}

	public void stop() throws OperationUnsupportedException
	{
		throw new UnsupportedOperationException();
	}

	public boolean isCancelSupported()
	{
		return false;
	}

	public boolean isStopSupported()
	{
		return false;
	}

	public ClientConfiguration getClientConfiguration(TargetModuleID arg0)
	{
		throw new UnsupportedOperationException();
	}
}
