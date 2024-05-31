package com.parkit.parkingsystem.integration.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;

public class DataBasePrepareService {

	private static final Logger logger = LogManager.getLogger("DataBasePrepareService");

	DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();

	public void clearDataBaseEntries() {

		logger.info("Clearing database entries...");

		Connection connection = null;
		PreparedStatement psTicket = null;
		PreparedStatement psParkingSpot = null;

		try {
			connection = dataBaseTestConfig.getConnection();

			// Clear table ticket in DB
			psTicket = connection.prepareStatement("TRUNCATE TABLE ticket");
			psTicket.executeUpdate();

			// Reset parking spot availability
			psParkingSpot = connection.prepareStatement("UPDATE parking SET available = true");
			psParkingSpot.executeUpdate();

			logger.info("Database entries cleared.");

		} catch (SQLException | ClassNotFoundException e) {
			e.printStackTrace();
			logger.error("Error clearing databse entries ", e);
		} finally {
			dataBaseTestConfig.closePreparedStatement(psTicket);
			dataBaseTestConfig.closePreparedStatement(psParkingSpot);
			dataBaseTestConfig.closeConnection(connection);
		}

	}

}
