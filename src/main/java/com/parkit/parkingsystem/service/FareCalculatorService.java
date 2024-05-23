package com.parkit.parkingsystem.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

	private static final Logger logger = LogManager.getLogger("FareCalculatorService");

	public void calculateFare(Ticket ticket) {
		calculateFare(ticket, false);
	}

	public void calculateFare(Ticket ticket, boolean discount) {
		if ((ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime()))) {
			logger.error("Out Time value is:  " + ticket.getOutTime());
			throw new IllegalArgumentException("Out time provided is incorrect:" + ticket.getOutTime().toString());
		}

		// Get time in milliseconds
		long inTimeInMillis = ticket.getInTime().getTime();
		long outTimeInMillis = ticket.getOutTime().getTime();

		double durationInHours = (double) (outTimeInMillis - inTimeInMillis) / (1000 * 60 * 60);

		if (durationInHours <= 0.5) {
			ticket.setPrice(0.0);
			logger.info("Parking time less than 30 minutes. Free parking. ");
			return;
		}

		// fare per hour
		double rate;
		switch (ticket.getParkingSpot().getParkingType()) {
		case CAR:
			rate = Fare.CAR_RATE_PER_HOUR;
			break;
		case BIKE:
			rate = Fare.BIKE_RATE_PER_HOUR;
			break;
		default:
			throw new IllegalArgumentException("Unkown Parking Type");
		}

		// total cost of parking
		double fare = durationInHours * rate;
		if (discount) { // if boolean discount is true (regular user)
			fare = fare * 0.95; // Regular user gets 5% discount
		}

		ticket.setPrice(fare);
		logger.info("Fare calculated: " + fare + " or ticket getPrice calculated: " + ticket.getPrice()
				+ " for duration (hours) : " + durationInHours + " with discount : " + discount);
	}
}