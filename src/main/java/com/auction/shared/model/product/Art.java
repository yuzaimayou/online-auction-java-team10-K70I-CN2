package com.auction.shared.model.product;

import java.time.LocalDateTime;

/**
 * Art is a specialized Item.
 *
 * With the current database schema, art records use the same columns as items,
 * so this class keeps constructor parity with Item for easy repository reuse.
 */
public class Art extends Item {

	// Constructor for creating a new art listing.
	public Art(String id, String name, String description,
			   double startingPrice,
			   LocalDateTime startTime,
			   LocalDateTime endTime,
			   String sellerId) {
		super(id, name, description, startingPrice, startTime, endTime, sellerId);
	}

	// Constructor for loading an existing art listing from database.
	public Art(String id, String name, String description,
			   double startingPrice,
			   double highestCurrentPrice,
			   LocalDateTime startTime,
			   LocalDateTime endTime,
			   String sellerId) {
		super(id, name, description, startingPrice, highestCurrentPrice, startTime, endTime, sellerId);
	}
}
