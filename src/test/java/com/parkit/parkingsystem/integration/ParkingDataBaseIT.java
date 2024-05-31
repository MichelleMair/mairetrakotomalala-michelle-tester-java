package com.parkit.parkingsystem.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.FareCalculatorService;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.service.SystemDateService;
import com.parkit.parkingsystem.util.InputReaderUtil;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ParkingDataBaseIT {

	private static final Logger logger = LogManager.getLogger("ParkingDataBaseIT");

	private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
	private static FareCalculatorService fareCalculatorService;
	private static ParkingSpotDAO parkingSpotDAO;
	private static TicketDAO ticketDAO;
	private static DataBasePrepareService dataBasePrepareService;

	@Mock
	private static InputReaderUtil inputReaderUtil;

	@Mock
	private static Ticket ticket;

	@Mock
	private static SystemDateService systemDateService;

	private long FIXED_IN_TIME;
	private long FIXED_OUT_TIME; // +1H

	@BeforeAll
	private static void setUp() throws Exception {
		fareCalculatorService = new FareCalculatorService(ticketDAO);

		parkingSpotDAO = new ParkingSpotDAO(dataBaseTestConfig);
		ticketDAO = new TicketDAO(dataBaseTestConfig);

		dataBasePrepareService = new DataBasePrepareService();
	}

	@BeforeEach
	private void setUpPerTest() throws Exception {
		MockitoAnnotations.initMocks(this);

		// Mock input values
		when(inputReaderUtil.readSelection()).thenReturn(1); // CAR
		when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");

		// Mock in Time and out Time values
		FIXED_IN_TIME = System.currentTimeMillis();
		FIXED_OUT_TIME = FIXED_IN_TIME + (3600 * 1000); // +1H

		when(systemDateService.getCurrentDate()).thenReturn(new Date(FIXED_IN_TIME))
				.thenReturn(new Date(FIXED_OUT_TIME));

		dataBasePrepareService.clearDataBaseEntries();
	}

	@AfterEach
	private void tearDownPerTest() {
		dataBasePrepareService.clearDataBaseEntries();
	}

	@AfterAll
	private static void tearDown() {
		dataBasePrepareService.clearDataBaseEntries();
	}

	@Test
	public void testParkingACar() {
		try {
			ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO,
					fareCalculatorService, systemDateService);

			parkingService.processIncomingVehicle();

			Ticket ticket = ticketDAO.getTicket("ABCDEF");
			assertNotNull(ticket, "Ticket should not be null");
			assertEquals("ABCDEF", ticket.getVehicleRegNumber());

			// Tolerance for timestamp difference 1000ms
			assertEquals(FIXED_IN_TIME, ticket.getInTime().getTime(), 1000,
					"In time should be close to the expected value");
			assertNull(ticket.getOutTime(), "At this stage, value of out time must be null");
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception : " + e.getMessage());
		}
	}

	@Test
	public void testParkingABike() {
		when(inputReaderUtil.readSelection()).thenReturn(2); // BIKE

		ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO,
				fareCalculatorService, systemDateService);

		parkingService.processIncomingVehicle();
		// TODO:
		// check that a ticket is actually saved in DB
		Ticket ticket = ticketDAO.getTicket("ABCDEF");
		assertNotNull(ticket, "Ticket should not be null");

		// Check if parking table in DB is updated
		ParkingSpot parkingSpot = ticket.getParkingSpot();
		assertNotNull(parkingSpot, "Parking spot should not be null");
		assertTrue(!parkingSpot.isAvailable(), "Parking spot should be marked as unavailable");
	}

	@Test
	public void testParkingLotExit() throws Exception {
		testParkingACar();

		ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO,
				fareCalculatorService, systemDateService);

		parkingService.processExitingVehicle();

		// Get the ticket
		Ticket ticket = ticketDAO.getTicket("ABCDEF");

		assertNotNull(ticket);
		assertNotNull(ticket.getOutTime(), "Ticket should not be null when exiting a vehicle");
		assertEquals(FIXED_OUT_TIME, ticket.getOutTime().getTime(), 1000,
				"Out Time should be close to the expected value");
		assertEquals(Fare.CAR_RATE_PER_HOUR, ticket.getPrice(), 0.01, "Fare must be greater than 0");
		assertEquals(1, parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR), "The exiting car leaves the slot 1 free");
	}

	@Test
	public void testParkingLotExitRecurringUser() throws Exception {
		testParkingLotExit();

		ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO,
				fareCalculatorService, systemDateService);

		// Second visit
		parkingService.processIncomingVehicle();
		Ticket secondVisitTicket = ticketDAO.getTicket("ABCDEF");

		assertNotNull(secondVisitTicket, "Second visit ticket should not be null.");
		assertNull(secondVisitTicket.getOutTime(), "Out time should be null for second visit.");

		// fast forward to simulate exit after 1 hour
		secondVisitTicket.setOutTime(new Date(FIXED_OUT_TIME)); // +1hour

		parkingService.processExitingVehicle();

		// Checked if there is an update
		secondVisitTicket = ticketDAO.getTicket("ABCDEF");

		// Calculate the expected fare with 5% discount
		double expectedFare = Math.round(Fare.CAR_RATE_PER_HOUR * 0.95 * 100.0) / 100.0;

		// expectedFare for regular user
		boolean regularUser = ticketDAO.getNbTicket(secondVisitTicket.getVehicleRegNumber()) > 1;
		if (regularUser) {
			secondVisitTicket.setPrice(expectedFare);
		}

		assertEquals(expectedFare, secondVisitTicket.getPrice(), 0.01, "5% discount for regular user");
		assertEquals(1, parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR), "The exiting car leaves the slot 1 free");
	}

}
