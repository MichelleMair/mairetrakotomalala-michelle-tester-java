package com.parkit.parkingsystem.service;

import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.util.InputReaderUtil;

public class ParkingService {

	private static final Logger logger = LogManager.getLogger("ParkingService");

	private InputReaderUtil inputReaderUtil;
	private ParkingSpotDAO parkingSpotDAO;
	private TicketDAO ticketDAO;
	private FareCalculatorService fareCalculatorService;
	private SystemDateService systemDateService;

	public ParkingService(InputReaderUtil inputReaderUtil, ParkingSpotDAO parkingSpotDAO, TicketDAO ticketDAO,
			FareCalculatorService fareCalculatorService, SystemDateService systemDateService) {
		this.inputReaderUtil = inputReaderUtil;
		this.parkingSpotDAO = parkingSpotDAO;
		this.ticketDAO = ticketDAO;
		this.fareCalculatorService = fareCalculatorService;
		this.systemDateService = systemDateService;
	}

	public void processIncomingVehicle() {
		try {
			ParkingSpot parkingSpot = getNextParkingNumberIfAvailable();

			if (parkingSpot != null && parkingSpot.getId() > 0) {
				String vehicleRegNumber = getVehicleRegNumber();

				logger.info("Processing incoming vehicle: " + vehicleRegNumber);

				parkingSpot.setAvailable(false);
				parkingSpotDAO.updateParking(parkingSpot);// allot this parking space and mark it's availability as
															// false

				Date inTime = systemDateService.getCurrentDate();
				Ticket ticket = new Ticket();

				ticket.setParkingSpot(parkingSpot);
				ticket.setVehicleRegNumber(vehicleRegNumber);
				ticket.setPrice(0);
				ticket.setInTime(inTime);

				ticketDAO.saveTicket(ticket);

				// Verify if the customer have more than 1 ticket (regular customer)
				boolean isRecurringUser = ticketDAO.getNbTicket(vehicleRegNumber) > 1;

				if (isRecurringUser) {
					System.out.println(
							"Welcome back! As a regular user of our parking lot, you'll benefit from 5% discount.");
				}

				System.out.println("Generated Ticket and saved in DB");
				System.out.println("Please park your vehicle in spot number:" + parkingSpot.getId());
				System.out.println("Recorded in-time for vehicle number:" + vehicleRegNumber + " is:" + inTime);
			} else {
				throw new Exception("Error fetching parking number from DB. Paking slots might be full");
			}
		} catch (Exception e) {
			logger.error("Unable to process incoming vehicle", e);
		}
	}

	public void processExitingVehicle() {
		try {
			String vehicleRegNumber = getVehicleRegNumber();
			Ticket ticket = ticketDAO.getTicket(vehicleRegNumber);

			if (ticket == null) {
				System.out.println("Ticket not found for vehicle registration number: " + vehicleRegNumber);
				return;
			}

			Date outTime = systemDateService.getCurrentDate();
			ticket.setOutTime(outTime);

			// Count tickets
			int nbTickets = ticketDAO.getNbTicket(vehicleRegNumber);
			boolean isRecurringUser = nbTickets > 1;

			double calculatedFare = fareCalculatorService.calculateFare(ticket, isRecurringUser);
			ticket.setPrice(calculatedFare);

			if (ticketDAO.updateTicket(ticket)) {

				ParkingSpot parkingSpot = ticket.getParkingSpot();
				parkingSpot.setAvailable(true);
				parkingSpotDAO.updateParking(parkingSpot);
				System.out.println("Thank you for your trust, please pay the parking fare:" + ticket.getPrice());
				System.out.println(
						"Recorded out-time for vehicle number:" + ticket.getVehicleRegNumber() + " is:" + outTime);
			} else {
				System.out.println("Unable to update ticket information. Error occurred");
			}
		} catch (Exception e) {
			logger.error("Unable to process exiting vehicle", e);
		}
	}

	private String getVehicleRegNumber() throws Exception {
		System.out.println("Please type the vehicle registration number and press enter key");
		return inputReaderUtil.readVehicleRegistrationNumber();
	}

	public ParkingSpot getNextParkingNumberIfAvailable() throws Exception {
		ParkingSpot parkingSpot = null;
		ParkingType parkingType = getVehicleType();
		try {
			int parkingNumber = parkingSpotDAO.getNextAvailableSlot(parkingType);
			if (parkingNumber > 0) {
				parkingSpot = new ParkingSpot(parkingNumber, parkingType, true);
			} else {
				throw new Exception("Error fetching parking number from DB. Parking slots might be full");
			}
		} catch (Exception e) {
			logger.error("Error fetching next available slot ", e);
		}
		return parkingSpot;
	}

	public ParkingType getVehicleType() {
		System.out.println("Please select vehicle type from menu");
		System.out.println("1 CAR");
		System.out.println("2 BIKE");
		int input = inputReaderUtil.readSelection();
		switch (input) {
		case 1:
			return ParkingType.CAR;
		case 2:
			return ParkingType.BIKE;
		default:
			System.out.println("Incorrect input provided");
			throw new IllegalArgumentException("Entered input is invalid");
		}
	}
}
