package com.parkit.parkingsystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

		verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
		verify(ticketDAO, Mockito.times(1)).updateTicket(any(Ticket.class));
		verify(ticketDAO, Mockito.times(1)).getNbTicket(anyString());
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
	// because the argument entered by the user regardung the vehicle type is
	// incorrect
	// test example: the user entered 3
	public void testGetNextParkingNumberIfAvailableParkingNumberWrongArgument() {
		when(inputReaderUtil.readSelection()).thenReturn(3);

		assertThrows(IllegalArgumentException.class, () -> parkingService.getNextParkingNumberIfAvailable());
	}
}
