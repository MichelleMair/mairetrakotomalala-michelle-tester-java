package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

	public void calculateFare(Ticket ticket) {
		if ((ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime()))) {
			throw new IllegalArgumentException("Out time provided is incorrect:" + ticket.getOutTime().toString());
		}

		// Get time in milliseconds (same as the logic in tests in
		// FareCalculatorServiceTest.java)
		long inTimeInMillis = ticket.getInTime().getTime();

		long outTimeInMillis = ticket.getOutTime().getTime();

		// TODO: Some tests are failing here. Need to check if this logic is correct
		/*
		 * Calculate parking time in minutes by subtracting the exit time
		 * (outTimeInMillis) from the entry time (inTimeInMillis) And divide the result
		 * by 60 000 to convert milliseconds to minutes
		 */
		long durationInMinutes = (outTimeInMillis - inTimeInMillis) / 60000;
		System.out.println("TEST temps duration FareCalculatorService: " + durationInMinutes);

		switch (ticket.getParkingSpot().getParkingType()) {
		case CAR: {
			// Implements the free fare if parking is less than 30 minutes
			if (durationInMinutes < 30) {
				ticket.setPrice(0);
			} else {
				// Dividing the fare per hour by 60 to have the fare per minute
				ticket.setPrice((durationInMinutes * Fare.CAR_RATE_PER_HOUR) / 60);
			}
			break;
		}
		case BIKE: {
			// Implements the free fare if parking is less than 30 minutes
			if (durationInMinutes < 30) {
				ticket.setPrice(0);
			} else {
				ticket.setPrice((durationInMinutes * Fare.BIKE_RATE_PER_HOUR) / 60);
			}
			break;
		}
		default:
			throw new IllegalArgumentException("Unkown Parking Type");
		}
	}
}