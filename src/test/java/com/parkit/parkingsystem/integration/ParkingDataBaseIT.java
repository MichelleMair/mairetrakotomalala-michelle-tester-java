package com.parkit.parkingsystem.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

	private static final Logger logger = LogManager.getLogger("ParkingDataBaseIT");

	private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
	private static ParkingSpotDAO parkingSpotDAO;
	private static TicketDAO ticketDAO;
	private static DataBasePrepareService dataBasePrepareService;
	private ParkingService parkingService;

	@Mock
	private static InputReaderUtil inputReaderUtil;

	@BeforeAll
	private static void setUp() throws Exception {
		parkingSpotDAO = new ParkingSpotDAO();
		parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
		ticketDAO = new TicketDAO();
		ticketDAO.dataBaseConfig = dataBaseTestConfig;
		dataBasePrepareService = new DataBasePrepareService();
	}

	@BeforeEach
	private void setUpPerTest() throws Exception {
		when(inputReaderUtil.readSelection()).thenReturn(1);
		when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");

		parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
	}

	@AfterAll
	private static void tearDown() {
		dataBasePrepareService.clearDataBaseEntries();
	}

	private void setOneHourParkingTime(Ticket ticket) {
		Date inTime = ticket.getInTime();
		Date outTime = new Date(inTime.getTime() + 60 * 60 * 1000); // +1 hour
		ticket.setOutTime(outTime);
		ticketDAO.updateTicket(ticket);
	}

	@Test
	public void testParkingACar() {
		try (Connection con = dataBaseTestConfig.getConnection()) {
			when(inputReaderUtil.readSelection()).thenReturn(1); // CAR
			ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
			parkingService.processIncomingVehicle();
			// TODO:
			// check that a ticket is actually saved in DB
			Ticket ticket = ticketDAO.getTicket("ABCDEF");
			assertNotNull(ticket, "Ticket should not be null");

			// Check if parking table in DB is updated
			ParkingSpot parkingSpot = ticket.getParkingSpot();
			assertNotNull(parkingSpot, "Parking spot should not be null");
			// assertEquals(false, parkingSpot.isAvailable(), "Parking spot is not
			// available.");
			assertTrue(!parkingSpot.isAvailable(), "Parking spot should be marked as unavailable");
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception : " + e.getMessage());
		}
	}

	@Test
	public void testParkingABike() {
		when(inputReaderUtil.readSelection()).thenReturn(2); // BIKE
		ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
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
	public void testParkingLotExit() {
		testParkingACar();
		ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

		// Get the ticket
		Ticket ticket = ticketDAO.getTicket("ABCDEF");
		logger.info("PARKINGDATABASEIT - testParkingLotExit() - Ticket 1 retrieved: " + ticket);
		assertNotNull(ticket, "Ticket should not be null");

		// Changing the parking time
		setOneHourParkingTime(ticket);

		// Call outgoing vehicle method
		parkingService.processExitingVehicle();

		// Retrieve the ticket again to check updated values
		ticket = ticketDAO.getTicket("ABCDEF");
		logger.info("PARKINGDATABASEIT - testParkingLotExit() - Ticket 2 retrieved: " + ticket);
		assertNotNull(ticket, "ticket should not be null");
		assertNotNull(ticket.getOutTime(), "Out time should be set");

		// Verify the fare
		logger.info("PARKINGDATABASEIT - testParkingLotExit() - ticket.getPrice : " + ticket.getPrice());
		assertTrue(ticket.getPrice() > 0, "Price should be set");

		// Verify parking spot availability
		ParkingSpot parkingSpot = ticket.getParkingSpot();
		assertNotNull(parkingSpot, "Parking spot should not be null");
		assertTrue(parkingSpot.isAvailable(), "Parking spot should be available after exit");

		logger.info("Test Parking Lot Exit - Ticket Price : " + ticket.getPrice());
		logger.info("Test Parking Lot Exit - Ticket OutTime : " + ticket.getOutTime());
	}

	@Test
	public void testParkingLotExitRecurringUser() {
		ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

		// First visit
		parkingService.processIncomingVehicle();
		Ticket firstVisitTicket = ticketDAO.getTicket("ABCDEF");
		setOneHourParkingTime(firstVisitTicket);
		logger.info("Ticket testParkingLotExitRecurringUser() first visit retrieved: " + firstVisitTicket);
		parkingService.processExitingVehicle();

		// Second visit
		parkingService.processIncomingVehicle();
		Ticket secondVisitTicket = ticketDAO.getTicket("ABCDEF");
		setOneHourParkingTime(secondVisitTicket);
		logger.info("Ticket testParkingLotExitRecurringUser() second visit retrieved: " + secondVisitTicket);
		parkingService.processExitingVehicle();

		// check if the 5% discount is really applying
		secondVisitTicket = ticketDAO.getTicket("ABCDEF");
		logger.info("Ticket secondVisitTicket: " + secondVisitTicket);

		assertNotNull(secondVisitTicket.getOutTime(), "Out time should be set");
		assertTrue(secondVisitTicket.getPrice() > 0, "Price should be set");

		logger.info("Ticket 5 % discount retrieved: " + secondVisitTicket);
		logger.info("Verify the fare for a regular user 2: " + secondVisitTicket.getPrice());

		double expectedPriceWithoutDiscount;
		switch (secondVisitTicket.getParkingSpot().getParkingType()) {
		case CAR:
			expectedPriceWithoutDiscount = Fare.CAR_RATE_PER_HOUR;
			break;
		case BIKE:
			expectedPriceWithoutDiscount = Fare.BIKE_RATE_PER_HOUR;
			break;
		default:
			throw new IllegalArgumentException("Unkown parking type");
		}

		double expectedPriceWithDiscount = expectedPriceWithoutDiscount * 0.95; // 5% discount
		assertEquals(expectedPriceWithDiscount, secondVisitTicket.getPrice(), 0.01,
				"Price should include 5% discount for regular user");
	}

}
