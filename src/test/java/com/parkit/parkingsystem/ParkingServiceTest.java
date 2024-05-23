package com.parkit.parkingsystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ParkingServiceTest {

	@Mock
	private static InputReaderUtil inputReaderUtil;
	@Mock
	private static ParkingSpotDAO parkingSpotDAO;
	@Mock
	private static TicketDAO ticketDAO;
	@InjectMocks
	private ParkingService parkingService;

	@BeforeEach
	private void setUpPerTest() {
		try {
			when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");

			ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
			Ticket ticket = new Ticket();
			ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000)));
			ticket.setParkingSpot(parkingSpot);
			ticket.setVehicleRegNumber("ABCDEF");

			when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
			when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);

			when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);

			parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to set up test mock objects");
		}
	}

	@Test
	// Mock the calling getNbTicket() method
	public void processExitingVehicleTest() {
		when(ticketDAO.getNbTicket(anyString())).thenReturn(2);

		parkingService.processExitingVehicle();

		verify(ticketDAO, Mockito.times(1)).getNbTicket(anyString());
		verify(ticketDAO, Mockito.times(1)).updateTicket(any(Ticket.class));
		verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
	}

	@Test
	// test the calling of the processIncomingVehicle() method
	public void testProcessIncomingVehicle() {
		when(inputReaderUtil.readSelection()).thenReturn(1);
		when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1);
		when(ticketDAO.saveTicket(any(Ticket.class))).thenReturn(true);

		parkingService.processIncomingVehicle();

		verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
		verify(ticketDAO, Mockito.times(1)).saveTicket(any(Ticket.class));
	}

	@Test
	// Execute the test in case the updateTiket() method return false
	// when the processExitingVehicle() is call
	public void processExitingVehicleTestUnableUpdate() {
		when(ticketDAO.getNbTicket(anyString())).thenReturn(2);
		when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(false);

		parkingService.processExitingVehicle();

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
	// When a vehicle is coming, and the driver is a regular user
	// check if the corresponding message is correctly logged
	public void testProcessIncomingVehicleRegularCustomer() throws Exception {
		when(inputReaderUtil.readSelection()).thenReturn(1);
		when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1);
		when(ticketDAO.saveTicket(any(Ticket.class))).thenReturn(true);
		when(ticketDAO.getNbTicket(anyString())).thenReturn(2);

		parkingService.processIncomingVehicle();

		verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
		verify(ticketDAO, Mockito.times(1)).saveTicket(any(Ticket.class));
		verify(ticketDAO, Mockito.times(1)).getNbTicket(anyString());
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
		when(ticketDAO.getNbTicket(anyString())).thenReturn(1);

		parkingService.processExitingVehicle();

		verify(ticketDAO, Mockito.times(1)).getNbTicket(anyString());
		verify(ticketDAO, Mockito.times(1)).updateTicket(any(Ticket.class));
		verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
	}

	@Test
	// Discount apply on regular user
	public void testProcessExitingVehicleDiscountedFare() {
		when(ticketDAO.getNbTicket(anyString())).thenReturn(2); // setup ticketDAO to return a ticket and a count of 2
																// for regular user
		Ticket ticket = new Ticket();
		ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000)));
		ticket.setOutTime(new Date());
		ticket.setParkingSpot(new ParkingSpot(1, ParkingType.CAR, false));
		ticket.setVehicleRegNumber("ABCDEF");
		when(ticketDAO.getTicket(anyString())).thenReturn(ticket);

		parkingService.processExitingVehicle();

		verify(ticketDAO, Mockito.times(1)).updateTicket(any(Ticket.class));
		assertTrue(ticket.getPrice() > 0);
	}

	@Test
	// Normal fare for non-regular user
	public void testProcessExitingVehicleRegularFare() {
		when(ticketDAO.getNbTicket(anyString())).thenReturn(1); // setup ticketDAO to return ticket, count of 1 for
																// non-regular user
		Ticket ticket = new Ticket();
		ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000)));
		ticket.setOutTime(new Date());
		ticket.setParkingSpot(new ParkingSpot(1, ParkingType.CAR, false));
		ticket.setVehicleRegNumber("ABCDEF");
		when(ticketDAO.getTicket(anyString())).thenReturn(ticket);

		parkingService.processExitingVehicle();

		verify(ticketDAO, Mockito.times(1)).updateTicket(any(Ticket.class));
		assertTrue(ticket.getPrice() > 0);
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
		when(ticketDAO.getNbTicket(anyString())).thenReturn(2);
		doThrow(new RuntimeException("Database error")).when(parkingSpotDAO).updateParking(any(ParkingSpot.class));

		parkingService.processExitingVehicle();

		verify(ticketDAO, Mockito.times(1)).getNbTicket(anyString());
		verify(ticketDAO, Mockito.times(1)).updateTicket(any(Ticket.class));
		verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
	}

	@Test
	// Test the exiting of a vehicle but unable to update ticket
	public void testProcessExitingVehicleUnableUpdate() {
		when(ticketDAO.getNbTicket(anyString())).thenReturn(2);
		when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(false);

		parkingService.processExitingVehicle();

		verify(parkingSpotDAO, Mockito.times(0)).updateParking(any(ParkingSpot.class));
	}

}
