package com.parkit.parkingsystem.integration.service;

import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;

public class DataBasePrepareService {

	DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
	private TicketDAO ticketDAO = new TicketDAO();
	private ParkingSpotDAO parkingSpotDAO = new ParkingSpotDAO();

	public void clearDataBaseEntries() {
		ticketDAO.dataBaseConfig = dataBaseTestConfig;
		parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
		ticketDAO.deleteAllTickets();
		parkingSpotDAO.resetParkingSpots();
	}

}
