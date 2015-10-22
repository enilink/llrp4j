/*
 * Copyright 2007 ETH Zurich
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */

package net.enilink.llrp4j.net;

import net.enilink.llrp4j.types.LlrpMessage;

/**
	 * The LLRPEndpoint interface needs to be implemented by any class
	 * that wants to receive LLRP messages asynchronously. 
 */

public interface LlrpEndpoint {

	/**
	 * The  messageReceived.message is called by the LLRPIoHandler whenever
	 * an LLRP message is received asynchrounly.
	 * 
	 * @param message LLRPmessage received
	 */
	public void messageReceived(LlrpMessage message);

	/**
	 * The method errorOccurred is called by the LLRPIoHandler whenever an error occurred.
	 * @param message 
	 */
	public void errorOccured(String message, Throwable cause);
}
