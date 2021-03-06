/**
 ** Copyright 2018 General Electric Company
 **
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at
 ** 
 **     http://www.apache.org/licenses/LICENSE-2.0
 ** 
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ** See the License for the specific language governing permissions and
 ** limitations under the License.
 */

package com.ge.research.semtk.load.utility;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class UriCache {
	static final String NOT_FOUND = "0-NOT-FOUND";
	static final String EMPTY_LOOKUP = "1-EMPTY-LOOKUP";
	
	ConcurrentHashMap<String, String> uriCache = new ConcurrentHashMap<String, String>();   // the cache
	ConcurrentHashMap<String, Boolean> notFound = new ConcurrentHashMap<String, Boolean>(); // any == NOT_FOUND
	ConcurrentHashMap<String, Boolean> isGenerated = new ConcurrentHashMap<String, Boolean>();  // any URI that was generated
	
	private String getKey(int importNodeIndex, ArrayList<String> builtStrings) {
		return String.valueOf(importNodeIndex) + "-" + String.join("-", builtStrings);
	}
	
	public void putUri(int importNodeIndex, ArrayList<String> builtStrings, String uri) {
		this.uriCache.putIfAbsent(this.getKey(importNodeIndex, builtStrings), uri);
	}
	
	public String getUri(int importNodeIndex, ArrayList<String> builtStrings) {
		try {
			return this.uriCache.get(this.getKey(importNodeIndex, builtStrings));
		} catch (Exception e) {
			return null;
		}
	}
	
	public boolean isNotFound(int importNodeIndex, ArrayList<String> builtStrings) {
		String key = this.getKey(importNodeIndex, builtStrings);
		return this.notFound.containsKey(key);
	}
	
	/**
	 * Declare a URI 'not found', possibly suggesting a new value
	 * @param importNodeIndex
	 * @param builtStrings
	 * @param generatedValue - value for URI ... or null or ""
	 */
	public synchronized void setUriNotFound(int importNodeIndex, ArrayList<String> builtStrings, String generatedValue) throws Exception {
		String key = this.getKey(importNodeIndex, builtStrings);
		String newVal;
		
		if (generatedValue != null && generatedValue.length() > 0) {
			newVal = generatedValue;
			this.isGenerated.put(newVal, true);
		} else {
			newVal = UriCache.NOT_FOUND;
		}
		
		// If already NOT_FOUND
		if (this.uriCache.containsKey(key)) {
			// already there: do nothing as along as this value is the same
			String curVal = this.uriCache.get(key);
			if (!newVal.equals(curVal)) {
				throw new Exception("Can't create a URI with two different values: " + curVal + " and " + newVal);
			}
		} else {
			// new: just put it in the cache
			this.uriCache.put(key, newVal);
		}
		this.notFound.putIfAbsent(key, true);
	}
	
	/**
	 * Generate GUIDS for all this.notFound keys that were not already generated by a mapping
	 * Remove them from this.notFound
	 * Add them to this.isGenerated
	 * @param uriResolver
	 * @throws Exception
	 */
	public void generateNotFoundURIs(UriResolver uriResolver) throws Exception {
		for (String key : this.notFound.keySet()) {
			if (this.uriCache.get(key).equals(UriCache.NOT_FOUND)) {
				String guid = uriResolver.generateRandomUri();
				this.uriCache.put(key, guid);
				this.isGenerated.put(guid, true);
			}
			
		}
		// clear out notFound hash
		this.notFound = new ConcurrentHashMap<String, Boolean>();
	}
	
	/**
	 * was this Uri generated by this.generateNotFoundGuids
	 * @param uri
	 * @return
	 */
	public boolean isGenerated(String uri) {
		if (uri == null || uri.isEmpty()) {
			return false;
		} else {
			return this.isGenerated.containsKey(uri);
		}
	}
	
}
