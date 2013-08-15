/*
 * This file is part of the RootFW Project: https://github.com/spazedog/rootfw
 *  
 * Copyright (c) 2013 Daniel Bergløv
 *
 * RootFW is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * RootFW is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public License
 * along with RootFW. If not, see <http://www.gnu.org/licenses/>
 */

package com.spazedog.lib.rootfw3.extenders;

import com.spazedog.lib.rootfw3.RootFW;
import com.spazedog.lib.rootfw3.interfaces.ExtenderGroup;

/**
 * This class is used to share the same RootFW instance across multiple classes and methods. It stores the instance in a static property, until some method disconnects from the shell.
 * <br />
 * It also has the option to lock a connection, which makes sure that it is not disconnected until it get's unlocked again. Each time lock is called, a additional lock is added, making it possible for multiple methods to keep a lock on the connection. First until all locks are removed can the connection be closed. This ensures that any method can call disconnect, without first checking if others are using the connection.
 * <br />
 * As this class is an extender, is should not be called directly. Instead use the static methods RootFW.userInstance() and RootFW.rootInstance() to get one of the shared instances.
 * 
 * <dl>
 * <dt><span class="strong">Example:</span></dt>
 * <dd><code><pre>
 * RootFW root = RootFW.rootInstance().lock().get();
 * 
 * if (root.isConnected()) {
 *     // ... Call other classes or methods which might also be using the shared instance ...
 *     
 *     RootFW.rootInstance().unlock().disconnect();
 * }
 * </pre></code></dd>
 * </dl>
 */
public class InstanceExtender implements ExtenderGroup {
	
	/**
	 * This is an extended class of the RootFW class.
	 * It's job is to implement the feature of not disconnecting locked connections. 
	 * The class is protected, so it is not possible to store instances as InstanceRootFW but only as RootFW, as the changes are only meant for internal purposes.
	 */
	protected static class InstanceRootFW extends RootFW {
		public Integer mLocks = 0;
		
		public InstanceRootFW(Boolean useRoot) {
			super(useRoot);
		}
		
		/**
		 * This will overwrite the disconnect() method in the main RootFW and implement feature of locking a connection.
		 */
		@Override
		public void disconnect() {
			if (mLocks == 0) {
				super.disconnect();
				InstanceExtender.onDisconnect();
			}
		}
	}
	
	/**
	 * Stores the user and root instances when created
	 */
	protected static InstanceRootFW[] oInstances = new InstanceRootFW[2];
	
	protected Integer mInstance;
	
	/**
	 * This is an internal callback method which get's called whenever an instance get's disconnected from the shell. This is used to remove the instance from the static container in order to avoid storing unused instances
	 */
	protected static void onDisconnect() {
		for (int i=0; i < oInstances.length; i++) {
			if (oInstances[i] != null && !oInstances[i].isConnected()) {
				oInstances[i] = null;
			}
		}
	}
	
	/**
	 * Create a new InstanceExtender instance. This will also create a new RootFW instance, if this has not already been done, and connect to the shell.
	 * 
	 * @param useRoot
	 *     Whether to use a root shell or a regular user shell
	 */
	public InstanceExtender(Boolean useRoot) {
		mInstance = useRoot ? 1 : 0;
		
		if (oInstances[mInstance] == null) {
			oInstances[mInstance] = new InstanceRootFW( mInstance == 1 );
			oInstances[mInstance].connect();
		}
	}
	
	/**
	 * Get the shared RootFW instance
	 * 
	 * @return
	 *     The stored RootFW instance
	 */
	public RootFW get() {
		return (RootFW) oInstances[mInstance];
	}
	
	/**
	 * Add a lock to the connection. This will make sure that the connection cannot be closed until the connection is unlocked()
	 * 
	 * @return
	 *     This instance
	 */
	public InstanceExtender lock() {
		if (oInstances[mInstance] != null) {
			oInstances[mInstance].mLocks += 1;
		}
		
		return this;
	}
	
	/**
	 * Remove the lock on the connection. Note that although you remove this lock, others might also have a lock added to the connection. These must also be removed in order to close the connection.
	 * 
	 * @return
	 *     This instance
	 */
	public InstanceExtender unlock() {
		if (oInstances[mInstance] != null && oInstances[mInstance].mLocks > 0) {
			oInstances[mInstance].mLocks -= 1;
		}
		
		return this;
	}
	
	/**
	 * Check whether there is any locks added to the connection
	 * 
	 * @return
	 *     <code>True</code> if there is locks added
	 */
	public Boolean isLocked() {
		return oInstances[mInstance] != null && oInstances[mInstance].mLocks > 0;
	}
	
	/**
	 * If you ever need to close a connection no matter how many locks has been added to it, you can use this method. It will remove all of the locks and force close the connection.
	 * <br />
	 * Note that this could provide issues if other methods are using this connection.
	 */
	public void destroy() {
		oInstances[mInstance].mLocks = 0;
		oInstances[mInstance].disconnect();
		oInstances[mInstance] = null;
	}
}