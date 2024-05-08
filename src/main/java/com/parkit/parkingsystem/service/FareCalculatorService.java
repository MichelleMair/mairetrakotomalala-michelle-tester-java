package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

	public void calculateFare(Ticket ticket) {
		if ((ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime()))) {
			throw new IllegalArgumentException("Out time provided is incorrect:" + ticket.getOutTime().toString());
		}

		// Obtenir le temps en millisecondes (comme dans FareCalculatorServiceTest.java)
		long inTimeInMillis = ticket.getInTime().getTime();

		long outTimeInMillis = ticket.getOutTime().getTime();

		// TODO: Some tests are failing here. Need to check if this logic is correct
		/*
		 * Calcul de la durée de stationnement en minutes en soustrayant le temps de
		 * sortie du temps d'entrée Et en divisant le résultat par 60 000 pour convertir
		 * les millisecondes en minutes
		 */
		long durationInMinutes = (outTimeInMillis - inTimeInMillis) / 60000;
		System.out.println("TEST temps duration FareCalculatorService: " + durationInMinutes);

		switch (ticket.getParkingSpot().getParkingType()) {
		case CAR: {
			// On divise le tarif horaire par 60 pour obtenir le tarif par minute
			ticket.setPrice((durationInMinutes * Fare.CAR_RATE_PER_HOUR) / 60);
			break;
		}
		case BIKE: {
			ticket.setPrice((durationInMinutes * Fare.BIKE_RATE_PER_HOUR) / 60);
			break;
		}
		default:
			throw new IllegalArgumentException("Unkown Parking Type");
		}
	}
}