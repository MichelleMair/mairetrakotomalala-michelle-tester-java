package com.parkit.parkingsystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.FareCalculatorService;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.service.SystemDateService;
import com.parkit.parkingsystem.util.InputReaderUtil;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ParkingServiceTest {

	private long FIXED_IN_TIME;
	private long FIXED_OUT_TIME;

	@Mock
	private static InputReaderUtil inputReaderUtil;
	@Mock
	private static ParkingSpotDAO parkingSpotDAO;
	@Mock
	private static TicketDAO ticketDAO;
	@Mock
	private SystemDateService systemDateService;
	@Mock
	private FareCalculatorService fareCalculatorService;

	@InjectMocks
	private ParkingService parkingService;

	@BeforeEach
	private void setUpPerTest() {
		// Mock fixed time for tests
		FIXED_IN_TIME = 1616927477011L;
		FIXED_OUT_TIME = FIXED_IN_TIME + (60 * 60 * 1000); // +1 heure

		fareCalculatorService = new FareCalculatorService(ticketDAO);
		parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, fareCalculatorService,
				systemDateService);
	}

	@Test
	// test the calling of the processIncomingVehicle() method
	public void testProcessIncomingVehicle() {
		try {
			ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);

			when(inputReaderUtil.readSelection()).thenReturn(1);
			when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1);
			when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true); // update
			when(systemDateService.getCurrentDate()).thenReturn(new Date(FIXED_IN_TIME)); // Date
			when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF"); // vehicle reg

		} catch (Exception e) {
			e.printStackTrace();
		}

		parkingService.processIncomingVehicle();

		verify(ticketDAO, Mockito.times(1)).saveTicket(any(Ticket.class));
		verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));

		// Create a ticket
		Ticket ticket = new Ticket();
		ticket.setInTime(new Date(FIXED_IN_TIME));
		ticket.setVehicleRegNumber("ABCDEF");
		ticket.setParkingSpot(new ParkingSpot(1, ParkingType.CAR, false));
		when(ticketDAO.getTicket(anyString())).thenReturn(ticket);

		// Get saved ticket
		Ticket savedTicket = ticketDAO.getTicket("ABCDEF");
		assertNotNull(savedTicket);
		assertEquals(new Date(FIXED_IN_TIME), savedTicket.getInTime());
		assertNull(savedTicket.getOutTime());
		assertEquals(0, savedTicket.getPrice());
		assertEquals("ABCDEF", savedTicket.getVehicleRegNumber());

		// Checked the parking spot update
		ParkingSpot updatedParkingSpot = savedTicket.getParkingSpot();
		assertNotNull(updatedParkingSpot);
		assertEquals(1, updatedParkingSpot.getId());
		assertEquals(ParkingType.CAR, updatedParkingSpot.getParkingType());
		assertFalse(updatedParkingSpot.isAvailable());
	}

	@Test
	// Mock the calling of exiting a vehicle
	public void processExitingVehicleTest() {
		try {
			ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
			Ticket ticket = new Ticket();
			ticket.setId(123);
			ticket.setInTime(new Date(FIXED_IN_TIME));
			ticket.setOutTime(new Date(FIXED_OUT_TIME));
			ticket.setVehicleRegNumber("ABCDEF");
			ticket.setParkingSpot(parkingSpot);

			when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF"); // vehicle registration number
			when(ticketDAO.getTicket("ABCDEF")).thenReturn(ticket); // get ticket for this vehicle reg number
			when(systemDateService.getCurrentDate()).thenReturn(new Date(FIXED_OUT_TIME)); // get out time
			when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true); // update the ticket
			when(ticketDAO.getNbTicket(anyString())).thenReturn(2); // Nb ticket for this vehicle reg number
			when(parkingSpotDAO.updateParking(parkingSpot)).thenReturn(true); // update the ticket
			when(fareCalculatorService.calculateFare(ticket, true)).thenReturn(Fare.CAR_RATE_PER_HOUR * 0.95);

		} catch (Exception e) {
			e.printStackTrace();
		}

		parkingService.processExitingVehicle();

		verify(ticketDAO, Mockito.times(1)).updateTicket(any(Ticket.class));
		verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));

		// Get the ticket
		Ticket updatedTicket = ticketDAO.getTicket("ABCDEF");
		assertNotNull(updatedTicket, "Ticket should not be null when exiting a vehicle");
		assertEquals(123, updatedTicket.getId(), "Id ticket should be 123");
		assertEquals("ABCDEF", updatedTicket.getVehicleRegNumber(), "Vehicle reg number must be ABCDEF");
		assertEquals(new Date(FIXED_IN_TIME), updatedTicket.getInTime(), "In Time for this ticket");
		assertEquals(new Date(FIXED_OUT_TIME), updatedTicket.getOutTime(), "Out time for this ticket");
		assertTrue(updatedTicket.getPrice() > 0);

		// checked if the parking spot was updated
		ParkingSpot updatedParkingSpot = updatedTicket.getParkingSpot();
		assertNotNull(updatedParkingSpot);
		assertTrue(updatedParkingSpot.isAvailable());
	}

	@Test
	// Execute the test in case the updateTiket() method return false
	// when the processExitingVehicle() is call
	public void processExitingVehicleTestUnableUpdate() {
		try {
			ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);

			Ticket ticket = new Ticket();
			ticket.setInTime(new Date(FIXED_IN_TIME));
			ticket.setOutTime(new Date(FIXED_OUT_TIME));
			ticket.setVehicleRegNumber("ABCDEF");
			ticket.setParkingSpot(parkingSpot);

			when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF"); // vehicle reg
			when(ticketDAO.getTicket("ABCDEF")).thenReturn(ticket);
			when(systemDateService.getCurrentDate()).thenReturn(new Date(FIXED_OUT_TIME)); // Date
			when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(false);

		} catch (Exception e) {
			e.printStackTrace();
		}
		parkingService.processExitingVehicle();

		verify(ticketDAO, Mockito.times(1)).updateTicket(any(Ticket.class));
		verify(parkingSpotDAO, Mockito.times(0)).updateParking(any(ParkingSpot.class));
	}

	@Test
	// test the calling of the getNextParkingNumberIfAvailable() method
	// with the result of obtaining a ID 1 spot available
	public void testGetNextParkingNumberIfAvailable() throws Exception {
		when(inputReaderUtil.readSelection()).thenReturn(1);
		when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1);

		ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();

		assertNotNull(parkingSpot);
		assertEquals(1, parkingSpot.getId());
		assertTrue(parkingSpot.isAvailable());
	}

	@Test
	// test the calling of the getNextParkingNumberIfAvailable() method with
	// the result no spots available (the method returns null)
	public void testGetNextParkingNumberIfAvailableParkingNumberNotFound() {
		when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(0);

		Exception exception = assertThrows(Exception.class, () -> parkingService.getNextParkingNumberIfAvailable());
		assertEquals("Entered input is invalid", exception.getMessage());
	}

	@Test
	// test the calling of the getNextParkingNumberIfAvailable() method with no
	// spots available
	// because the argument entered by the user regarding the vehicle type is
	// incorrect
	// test example: the user entered 3
	public void testGetNextParkingNumberIfAvailableParkingNumberWrongArgument() {
		when(inputReaderUtil.readSelection()).thenReturn(3);

		assertThrows(IllegalArgumentException.class, () -> parkingService.getNextParkingNumberIfAvailable());
	}

	@Test
	public void testGetVehicleTypeCar() throws Exception {
		when(inputReaderUtil.readSelection()).thenReturn(1);
		ParkingType type = parkingService.getVehicleType();
		assertEquals(ParkingType.CAR, type);
	}

	@Test
	public void testGetVehicleTypeBike() throws Exception {
		when(inputReaderUtil.readSelection()).thenReturn(2);
		ParkingType type = parkingService.getVehicleType();
		assertEquals(ParkingType.BIKE, type);
	}

	@Test
	public void testGetVehicleTypeInvalid() throws Exception {
		when(inputReaderUtil.readSelection()).thenReturn(3); // invalid selection
		assertThrows(IllegalArgumentException.class, () -> parkingService.getVehicleType());
	}

	@Test
	// Test for incoming vehicle and no available parking spot
	public void testProcessIncomingVehicleNoAvailableSPot() throws Exception {
		when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(0);

		parkingService.processIncomingVehicle();

		verify(parkingSpotDAO, Mockito.times(0)).updateParking(any(ParkingSpot.class));
		verify(ticketDAO, Mockito.times(0)).saveTicket(any(Ticket.class));
	}

	@Test
	// Test for exiting vehicle and the ticket is not found
	public void testProcessExitingVehicleTicketNotFound() throws Exception {
		when(ticketDAO.getTicket(anyString())).thenReturn(null);

		parkingService.processExitingVehicle();

		verify(ticketDAO, Mockito.times(0)).updateTicket(any(Ticket.class));
		verify(parkingSpotDAO, Mockito.times(0)).updateParking(any(ParkingSpot.class));
	}

	@Test
	// Test for incoming vehicle and an exception is thrown when trying to
	// get an available parking spot
	public void testProcessIncomingVehicleException() throws Exception {
		when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class)))
				.thenThrow(new RuntimeException("Database error"));

		parkingService.processIncomingVehicle();

		verify(parkingSpotDAO, Mockito.times(0)).updateParking(any(ParkingSpot.class));
		verify(ticketDAO, Mockito.times(0)).saveTicket(any(Ticket.class));
	}

	@Test
	// Test for exiting vehicle when an exception is thrown
	public void testProcessExitingVehicleException() throws Exception {
		when(ticketDAO.getTicket(anyString())).thenThrow(new RuntimeException("Database error"));

		parkingService.processExitingVehicle();

		verify(ticketDAO, Mockito.times(0)).updateTicket(any(Ticket.class));
		verify(parkingSpotDAO, Mockito.times(0)).updateParking(any(ParkingSpot.class));
	}

	@Test
	// Thrown an exception when trying to save ticket for an incoming vehicle
	public void testProcessIncomingVehicleSaveTicketException() throws Exception {
		when(inputReaderUtil.readSelection()).thenReturn(1);
		when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1);
		doThrow(new RuntimeException("Database error")).when(ticketDAO).saveTicket(any(Ticket.class));

		parkingService.processIncomingVehicle();

		verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
		verify(ticketDAO, Mockito.times(1)).saveTicket(any(Ticket.class));
	}

	@Test
	// If an Exception thrown when the method 'getVehicleRegNumber' is called
	public void testProcessIncomingVehicleRegNumberException() throws Exception {
		when(inputReaderUtil.readSelection()).thenReturn(1);
		when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1);
		when(inputReaderUtil.readVehicleRegistrationNumber())
				.thenThrow(new RuntimeException("Read vehicle registration number error"));

		parkingService.processIncomingVehicle();

		verify(parkingSpotDAO, Mockito.times(0)).updateParking(any(ParkingSpot.class));
		verify(ticketDAO, Mockito.times(0)).saveTicket(any(Ticket.class));
	}

	@Test
	public void testProcessExitingVehicleNotRegularUser() throws Exception {
		try {
			ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
			Ticket ticket = new Ticket();
			ticket.setInTime(new Date(FIXED_IN_TIME));
			ticket.setOutTime(new Date(FIXED_OUT_TIME));
			ticket.setParkingSpot(parkingSpot);
			ticket.setVehicleRegNumber("ABCDEF");

			when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF"); // vehicle reg number
			when(ticketDAO.getTicket("ABCDEF")).thenReturn(ticket); // get ticket for this vehicle reg number
			when(systemDateService.getCurrentDate()).thenReturn(new Date(FIXED_OUT_TIME)); // get out time
			when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true); // get updated ticket
			when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true); // updated ticket
			when(ticketDAO.getNbTicket(anyString())).thenReturn(1);
			when(fareCalculatorService.calculateFare(ticket, false)).thenReturn(Fare.CAR_RATE_PER_HOUR);

		} catch (Exception e) {
			e.printStackTrace();
		}
		parkingService.processExitingVehicle();

		verify(ticketDAO, Mockito.times(1)).getNbTicket(anyString());
		verify(ticketDAO, Mockito.times(1)).updateTicket(any(Ticket.class));
		verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));

		Ticket updatedTicket = ticketDAO.getTicket("ABCDEF");
		assertNotNull(updatedTicket);
		assertEquals(Fare.CAR_RATE_PER_HOUR, updatedTicket.getPrice(), 0.01);
	}

	@Test
	// Discount apply on regular user
	public void testProcessExitingVehicleDiscountedFare() {
		try {
			ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);

			Ticket ticket = new Ticket();
			ticket.setInTime(new Date(FIXED_IN_TIME));
			ticket.setOutTime(new Date(FIXED_OUT_TIME));
			ticket.setParkingSpot(parkingSpot);
			ticket.setVehicleRegNumber("ABCDEF");

			when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF"); // vehicle reg number
			when(ticketDAO.getTicket("ABCDEF")).thenReturn(ticket); // get ticket for this vehicle reg number
			when(systemDateService.getCurrentDate()).thenReturn(new Date(FIXED_OUT_TIME)); // get out time
			when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true); // get updated ticket
			when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true); // updated ticket
			when(ticketDAO.getNbTicket(anyString())).thenReturn(2);
			when(fareCalculatorService.calculateFare(ticket, true)).thenReturn(Fare.CAR_RATE_PER_HOUR * 0.95);

		} catch (Exception e) {
			e.printStackTrace();
		}
		parkingService.processExitingVehicle();

		verify(ticketDAO, Mockito.times(1)).updateTicket(any(Ticket.class));
		verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));

		Ticket updatedTicket = ticketDAO.getTicket("ABCDEF");
		assertNotNull(updatedTicket, "Ticket should not be null when exiting a vehicle");
		assertEquals(new Date(FIXED_IN_TIME), updatedTicket.getInTime());
		assertEquals(new Date(FIXED_OUT_TIME), updatedTicket.getOutTime());
		assertEquals((Fare.CAR_RATE_PER_HOUR) * 0.95, updatedTicket.getPrice(), 0.01, "5% discount for regular user");
	}

	@Test
	// Normal fare for non-regular user
	public void testProcessExitingVehicleRegularFare() {
		try {
			ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);

			Ticket ticket = new Ticket();
			ticket.setInTime(new Date(FIXED_IN_TIME));
			ticket.setOutTime(new Date(FIXED_OUT_TIME));
			ticket.setParkingSpot(parkingSpot);
			ticket.setVehicleRegNumber("ABCDEF");

			when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF"); // vehicle reg number
			when(ticketDAO.getTicket("ABCDEF")).thenReturn(ticket); // get ticket for this vehicle reg number
			when(systemDateService.getCurrentDate()).thenReturn(new Date(FIXED_OUT_TIME)); // get out time
			when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true); // get updated ticket
			when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true); // updated ticket
			when(ticketDAO.getNbTicket(anyString())).thenReturn(1); // not a regular user
			when(fareCalculatorService.calculateFare(ticket, false)).thenReturn(Fare.CAR_RATE_PER_HOUR);

		} catch (Exception e) {
			e.printStackTrace();
		}
		parkingService.processExitingVehicle();

		verify(ticketDAO, Mockito.times(1)).updateTicket(any(Ticket.class));
		verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));

		Ticket updatedTicket = ticketDAO.getTicket("ABCDEF");
		assertNotNull(updatedTicket, "Ticket shoul not be null when exiting a vehicle");
		assertEquals(new Date(FIXED_IN_TIME), updatedTicket.getInTime());
		assertEquals(new Date(FIXED_OUT_TIME), updatedTicket.getOutTime());
		assertTrue(updatedTicket.getPrice() > 0);
	}

	@Test
	// If an Exception thrown when the method 'getNextParkingNumberIfAvailable' is
	// called
	public void testProcessIncomingVehicleGetNextParkingNumberIfAvailableException() throws Exception {
		when(inputReaderUtil.readSelection()).thenReturn(1);
		when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class)))
				.thenThrow(new RuntimeException("Database error"));

		parkingService.processIncomingVehicle();

		verify(parkingSpotDAO, Mockito.times(0)).updateParking(any(ParkingSpot.class));
		verify(ticketDAO, Mockito.times(0)).saveTicket(any(Ticket.class));
	}

	@Test
	// An exception is thrown when a parking spot is updated
	public void testProcessExitingVehicleUpdateParkingException() throws Exception {
		try {
			ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);

			Ticket ticket = new Ticket();
			ticket.setInTime(new Date(FIXED_IN_TIME));
			ticket.setOutTime(new Date(FIXED_OUT_TIME));
			ticket.setParkingSpot(parkingSpot);
			ticket.setVehicleRegNumber("ABCDEF");

			when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF"); // vehicle reg number
			when(ticketDAO.getTicket("ABCDEF")).thenReturn(ticket); // get ticket for this vehicle reg number
			when(systemDateService.getCurrentDate()).thenReturn(new Date(FIXED_OUT_TIME)); // get out time
			when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true); // get updated ticket
			doThrow(new RuntimeException("Database error")).when(parkingSpotDAO).updateParking(any(ParkingSpot.class));
			when(ticketDAO.getNbTicket(anyString())).thenReturn(2);

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Failing the process of exiting a vehicle");
		}

		parkingService.processExitingVehicle();

		verify(ticketDAO, Mockito.times(1)).updateTicket(any(Ticket.class));
		verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
	}

	@Test
	// Test the exiting of a vehicle but unable to update ticket
	public void testProcessExitingVehicleUnableUpdate() {
		try {
			ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);

			Ticket ticket = new Ticket();
			ticket.setInTime(new Date(FIXED_IN_TIME));
			ticket.setOutTime(new Date(FIXED_OUT_TIME));
			ticket.setParkingSpot(parkingSpot);
			ticket.setVehicleRegNumber("ABCDEF");

			when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF"); // vehicle reg number
			when(ticketDAO.getTicket("ABCDEF")).thenReturn(ticket); // get ticket for this vehicle reg number
			when(systemDateService.getCurrentDate()).thenReturn(new Date(FIXED_OUT_TIME)); // get out time
			when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(false); // get updated ticket
			when(ticketDAO.getNbTicket(anyString())).thenReturn(2);

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Failing the process of exiting a vehicle");
		}

		parkingService.processExitingVehicle();

		verify(ticketDAO, Mockito.times(1)).updateTicket(any(Ticket.class));
		verify(parkingSpotDAO, Mockito.times(0)).updateParking(any(ParkingSpot.class));
	}

}
