/**
 ** Copyright 2016 General Electric Company
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

package com.ge.research.semtk.belmont.test;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ge.research.semtk.test.TestGraph;
import com.ge.research.semtk.belmont.AutoGeneratedQueryTypes;
import com.ge.research.semtk.belmont.NodeGroup;
import com.ge.research.semtk.load.utility.SparqlGraphJson;
import com.ge.research.semtk.sparqlX.SparqlEndpointInterface;
import com.ge.research.semtk.sparqlX.SparqlResultTypes;

public class LoadAndQueryGenTest_IT {
	
	private static final String qry = "select * where {?x ?y ?z .} limit 10";
	
	private static SparqlGraphJson sgJson = null;
	
	@BeforeClass
	public static void beforeClass() throws Exception {
		sgJson = TestGraph.initGraphWithData("sampleBattery");
	}
	
	
	@Test
	public void checkSparqlEndpointInterfaceCreation() throws Exception {

		SparqlEndpointInterface sei =  sgJson.getSparqlConn().getDataInterface(0);
		sei.executeQuery(qry, SparqlResultTypes.TABLE);
		assertEquals(sei.getResponse().toString().length(),2344);  	// character count of response		
	}
	
	@Test
	public void generateAndRunSelectQuery() throws Exception {
		
		NodeGroup ng = TestGraph.getNodeGroup("src/test/resources/sampleBattery.json");
		
		String select = ng.generateSparql(AutoGeneratedQueryTypes.QUERY_DISTINCT, null, 10, null);
		SparqlEndpointInterface sei =  sgJson.getSparqlConn().getDataInterface(0);
		sei.executeQuery(select, SparqlResultTypes.TABLE);
		
		assertEquals(sei.getResponse().toString().length(),966);	// character count of response
	}
	
}
