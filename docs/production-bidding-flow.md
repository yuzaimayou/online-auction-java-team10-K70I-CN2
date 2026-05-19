# Production Bidding Flow

Production bid placement is handled by the database-backed socket flow:

```text
ClientHandler -> BidService -> AutoBidResolver -> AuctionRoomManager
```

- `BidService` validates and mutates accepted bids inside database transactions.
- `AutoBidResolver` selects the next server-side auto-bid candidate.
- `AuctionRoomManager` broadcasts committed bid and auction-extension events to room clients.

Prototype/demo classes such as `BiddingService`, `AutoBiddingService`,
`AntiSnipingService`, `RealtimeBidUpdateService`, `Auction`, and `AutoBid` are
kept for historical OOP/design-pattern demonstrations. They are not part of the
production auction correctness path and should not be used by graders to assess
the active database-backed bidding flow.
