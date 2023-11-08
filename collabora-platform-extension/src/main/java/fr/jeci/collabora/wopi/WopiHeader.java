/*
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package fr.jeci.collabora.wopi;

public interface WopiHeader {
	String X_LOOL_WOPI_IS_AUTOSAVE = "X-LOOL-WOPI-IsAutosave";
	String X_LOOL_WOPI_TIMESTAMP = "X-LOOL-WOPI-Timestamp";
	String X_WOPI_OVERRIDE = "X-WOPI-Override";
	String X_WOPI_LOCK = "X-WOPI-Lock";
	String X_WOPI_OLD_LOCK = "X-WOPI-OldLock";
	String X_WOPI_LOCK_FAILURE_REASON = "X-WOPI-LockFailureReason";
	String X_WOPI_ITEM_VERSION = "X-WOPI-ItemVersion";

	String X_PRISTY_ADD_PROPERTY = "X-PRISTY-ADD-PROPERTY";
	String X_PRISTY_DEL_PROPERTY = "X-PRISTY-DEL-PROPERTY";
	String X_PRISTY_DEL_ASPECT = "X-PRISTY-DEL-ASPECT";
	String X_PRISTY_ADD_ASPECT = "X-PRISTY-ADD-ASPECT";
}
