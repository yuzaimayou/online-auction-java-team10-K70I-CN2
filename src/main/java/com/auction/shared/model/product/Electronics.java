package com.auction.shared.model.product;

import java.time.LocalDateTime;

/**
 * Electronics is a specialized Item.
 *
 * With the current database schema, electronics records use the same columns
 * as items, so this class keeps constructor parity with Item.
 */
public class Electronics extends Item {

	// Constructor for creating a new electronics listing.
	public Electronics(String id, String name, String description,
					   double startingPrice,
					   LocalDateTime startTime,
					   LocalDateTime endTime,
					   String sellerId) {
		super(id, name, description, startingPrice, startTime, endTime, sellerId);
	}

	// Constructor for loading an existing electronics listing from database.
	public Electronics(String id, String name, String description,
					   double startingPrice,
					   double highestCurrentPrice,
					   LocalDateTime startTime,
					   LocalDateTime endTime,
					   String sellerId) {
		super(id, name, description, startingPrice, highestCurrentPrice, startTime, endTime, sellerId);
	}
}

