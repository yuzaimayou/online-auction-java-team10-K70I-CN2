package com.auction.shared.model.product;

import java.time.LocalDateTime;

/**
 * Vehicle is a specialized Item.
 *
 * With the current database schema, vehicle records use the same columns
 * as items, so this class keeps constructor parity with Item.
 */
public class Vehicle extends Item {

	// Constructor for creating a new vehicle listing.
	public Vehicle(String id, String name, String description,
				   double startingPrice,
				   LocalDateTime startTime,
				   LocalDateTime endTime,
				   String sellerId) {
		super(id, name, description, startingPrice, startTime, endTime, sellerId);
	}

	// Constructor for loading an existing vehicle listing from database.
	public Vehicle(String id, String name, String description,
				   double startingPrice,
				   double highestCurrentPrice,
				   LocalDateTime startTime,
				   LocalDateTime endTime,
				   String sellerId) {
		super(id, name, description, startingPrice, highestCurrentPrice, startTime, endTime, sellerId);
	}
}
