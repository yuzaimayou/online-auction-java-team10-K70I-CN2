package com.auction.shared.model;

public class Bidder extends User {
    protected double balance;

    public Bidder(String id, String username, String password) {
        super(id, username, password, "Bidder");
        this.balance = 0.0;
    }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    // Pricing function (Self-purchase error has been blocked)
    public void placeBid(Item item, double amount) {
        if (item.isOwner(this.getId())) {
            System.err.println("Erro: " + this.username + " cannot set prices for their own products.!");
            return;
        }

        if (amount <= item.getHighestCurrentPrice()) {
            System.err.println("Erro: Setting price (" + amount + ") must be greater than (" + item.getHighestCurrentPrice() + ")!");
            return;
        }

        item.setHighestCurrentPrice(amount);
        System.out.println("Success: " + this.username + " sets price " + amount + " for item: " + item.getName());
    }
}