package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

	public void calculateFare(Ticket ticket) {
		calculateFare(ticket, false);
	}

	public void calculateFare(Ticket ticket, boolean discount) {
		if ((ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime()))) {
			throw new IllegalArgumentException("Out time provided is incorrect:" + ticket.getOutTime().toString());
		}

		// Get time in milliseconds (same as the logic in tests in
		// FareCalculatorServiceTest.java)
		long inTimeInMillis = ticket.getInTime().getTime();

		long outTimeInMillis = ticket.getOutTime().getTime();

		/*
		 * Calculate parking time in minutes by subtracting the exit time
		 * (outTimeInMillis) from the entry time (inTimeInMillis) And divide the result
		 * by 60 000 to convert milliseconds to minutes
		 */
		long durationInMinutes = (outTimeInMillis - inTimeInMillis) / 60000;

		// fare per hour
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
		double fare = 0;

		if (durationInMinutes < 30) { // free fare for parking lot less than 30 minutes
			fare = 0;
		} else {
			fare = (durationInMinutes * ratePerHour) / 60;
		}

		if (discount) { // if boolean discount is true (regular user)
			fare = fare * 0.95; // Regular user gets 5% discount
		}

		ticket.setPrice(fare);
	}
}