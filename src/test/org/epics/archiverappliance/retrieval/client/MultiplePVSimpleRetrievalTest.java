/*******************************************************************************
 * Copyright (c) 2011 The Board of Trustees of the Leland Stanford Junior University
 * as Operator of the SLAC National Accelerator Laboratory.
 * Copyright (c) 2011 Brookhaven National Laboratory.
 * EPICS archiver appliance is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 *******************************************************************************/
package org.epics.archiverappliance.retrieval.client;


import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;

import org.apache.log4j.Logger;
import org.epics.archiverappliance.Event;
import org.epics.archiverappliance.EventStream;
import org.epics.archiverappliance.EventStreamDesc;
import org.epics.archiverappliance.IntegrationTests;
import org.epics.archiverappliance.TomcatSetup;
import org.epics.archiverappliance.common.TimeUtils;
import org.epics.archiverappliance.config.ArchDBRTypes;
import org.epics.archiverappliance.config.ConfigServiceForTests;
import org.epics.archiverappliance.retrieval.GenerateData;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Test retrieval for multiple PVs
 * @author mshankar
 *
 */
@Category(IntegrationTests.class)
public class MultiplePVSimpleRetrievalTest {
	private static final Logger logger = Logger.getLogger(MultiplePVSimpleRetrievalTest.class.getName());
	TomcatSetup tomcatSetup = new TomcatSetup();
	private static int TOTAL_NUMBER_OF_PVS = 10;
	private static String[] pvs = new String[TOTAL_NUMBER_OF_PVS];


	@Before
	public void setUp() throws Exception {
		int phasediff = 360/TOTAL_NUMBER_OF_PVS;
		for(int i = 0; i < TOTAL_NUMBER_OF_PVS; i++) {
			pvs[i] = ConfigServiceForTests.ARCH_UNIT_TEST_PVNAME_PREFIX + "Sine" + i;
			GenerateData.generateSineForPV(pvs[i], i*phasediff, ArchDBRTypes.DBR_SCALAR_DOUBLE);
		}
		tomcatSetup.setUpWebApps(this.getClass().getSimpleName());
	}

	@After
	public void tearDown() throws Exception {
		tomcatSetup.tearDown();
	}

	static long previousEpochSeconds = 0; 
	@Test
	public void testGetDataForMultiplePVs() {
		RawDataRetrievalAsEventStream rawDataRetrieval = new RawDataRetrievalAsEventStream("http://localhost:" + ConfigServiceForTests.RETRIEVAL_TEST_PORT+ "/retrieval/data/getData.raw");
		Timestamp start = TimeUtils.convertFromISO8601String("2011-02-01T08:00:00.000Z");
		Timestamp end = TimeUtils.convertFromISO8601String("2011-02-02T08:00:00.000Z");
		EventStream stream = null;
		try {
			stream = rawDataRetrieval.getDataForPVS(pvs, start, end, new RetrievalEventProcessor() {
				@Override
				public void newPVOnStream(EventStreamDesc desc) {
					logger.info("On the client side, switching to processing PV " + desc.getPvName());
					previousEpochSeconds = 0;
				}
			});


			// We are making sure that the stream we get back has times in sequential order...
			if(stream != null) {
				for(Event e : stream) {
					long actualSeconds = e.getEpochSeconds();
					assertTrue(actualSeconds >= previousEpochSeconds);
					previousEpochSeconds = actualSeconds;
				}
			}
		} finally {
			if(stream != null) try { stream.close(); stream = null; } catch(Throwable t) { }
		}
	}	
}
