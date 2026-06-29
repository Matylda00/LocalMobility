package com.rozkladjazdy.jazdaz.dtos;

public record PurchaseTicketRequest(
        String ticketName,
        String ticketCategory,
        String cardNumber,
        String expiryDate,
        String cvv,
        String cardHolder
) {
}