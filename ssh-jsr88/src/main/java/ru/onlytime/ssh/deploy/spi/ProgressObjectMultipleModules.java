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

public class ProgressObjectMultipleModules implements ProgressObject {

	private List<ProgressListener> listeners = new LinkedList<ProgressListener>();
	private List<DeploymentStatus> statuses = new LinkedList<DeploymentStatus>();
	private List<TargetModuleID> modules = new LinkedList<TargetModuleID>();

	public void addModuleProcessResult(DeploymentStatus status, TargetModuleID module) {
		statuses.add(status);
		modules.add(module);
	}

	@Override
	public void addProgressListener(ProgressListener listener) {
		listeners.add(listener);
		for (int i = 0; i < statuses.size(); i++) {
			listener.handleProgressEvent(new ProgressEvent(this,modules.get(i),statuses.get(i)));
		}
	}

	@Override
	public DeploymentStatus getDeploymentStatus() {
		//status of this activity. suppose last.
		return statuses.get(statuses.size()-1);
	}

	@Override
	public TargetModuleID[] getResultTargetModuleIDs() {
		return modules.toArray(new TargetModuleID[0]);
	}

	@Override
	public void removeProgressListener(ProgressListener listener) {
		listeners.remove(listener);
	}

	public void cancel() throws OperationUnsupportedException {
		throw new UnsupportedOperationException();
	}

	public void stop() throws OperationUnsupportedException {
		throw new UnsupportedOperationException();
	}

	public boolean isCancelSupported() {
		return false;
	}

	public boolean isStopSupported() {
		return false;
	}

	public ClientConfiguration getClientConfiguration(TargetModuleID arg0) {
		throw new UnsupportedOperationException();
	}

}
