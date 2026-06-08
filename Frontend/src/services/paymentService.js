export async function processPayment(paymentData) {
  await new Promise((resolve) => setTimeout(resolve, 1000));

  const cardNumber = paymentData.cardNumber.replaceAll(" ", "");

  if (cardNumber.length !== 16) {
    return {
      success: false,
      message: "Numer karty musi mieć 16 cyfr.",
    };
  }

  if (cardNumber === "4000000000000002") {
    return {
      success: false,
      message: "Płatność została odrzucona przez mockowy system płatności.",
    };
  }

  return {
    success: true,
    transactionId: crypto.randomUUID(),
    message: "Płatność zaakceptowana.",
  };
}