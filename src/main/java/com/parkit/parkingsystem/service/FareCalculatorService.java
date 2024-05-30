package com.parkit.parkingsystem.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.parkit.parkingsystem.config.DataBaseConfig;
import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

	public DataBaseConfig dataBaseConfig = new DataBaseConfig();
	private TicketDAO ticketDAO = new TicketDAO(dataBaseConfig);

	private static final Logger logger = LogManager.getLogger("FareCalculatorService");

	public FareCalculatorService(TicketDAO ticketDAO) {
		this.ticketDAO = ticketDAO;
	}

	public void calculateFare(Ticket ticket) {
		calculateFare(ticket, false);
	}

	public double calculateFare(Ticket ticket, boolean discount) {
		if ((ticket.getOutTime() == null)) {
			throw new IllegalArgumentException("Out time provided is null:" + ticket.getOutTime().toString());
		}

		if (ticket.getOutTime().before(ticket.getInTime())) {
			throw new IllegalArgumentException(
					"Out time provided is before in time, incorrect :" + ticket.getOutTime().toString());
		}

		// Get time in milliseconds
		long inTimeInMillis = ticket.getInTime().getTime();
		long outTimeInMillis = ticket.getOutTime().getTime();

		long durationInMillis = outTimeInMillis - inTimeInMillis;

		double durationInHours = (double) durationInMillis / (1000 * 60 * 60);

		if (durationInMillis < (30 * 60 * 1000)) {
			ticket.setPrice(0);
			logger.info("Parking time less than 30 minutes. Free parking. ");
			return 0.0;
		}

		// rate per hour
		double ratePerHour;
		switch (ticket.getParkingSpot().getParkingType()) {
		case CAR:
			ratePerHour = Fare.CAR_RATE_PER_HOUR;
			break;
		case BIKE:
			ratePerHour = Fare.BIKE_RATE_PER_HOUR;
			break;
		default:
			throw new IllegalArgumentException("Unkown Parking Type");
		}

		// total cost of parking
		double fare = durationInHours * ratePerHour;
		if (discount) { // if boolean discount is true (regular user)
			fare = fare * 0.95; // Regular user gets 5% discount
		}

		// Round fare to the nearest cent
		fare = Math.round(fare * 100.0) / 100.0;

		ticket.setPrice(fare);
		logger.info("Fare calculated: " + fare + " or ticket getPrice calculated: " + ticket.getPrice()
				+ " for duration (hours) : " + durationInHours + " with discount : " + discount);
		return fare;
	}
}