export interface OriginalRetailPrice {
  gross: number;
  net: string;
  vat: string;
}

function validMoney(value: string): number | null {
  if (value.trim() === '') return null;
  const number = Number(value);
  return Number.isFinite(number) && number >= 0 ? number : null;
}

function validVat(value: string): number | null {
  if (value.trim() === '') return null;
  const number = Number(value);
  return Number.isFinite(number) && number >= 0 && number <= 100 ? number : null;
}

// ProductDto.sellPrice is the customer price including VAT. Keep the existing
// API contract while allowing managers to enter the pre-VAT price in the form.
export function calculateStoredSellPrice(
  netPrice: string,
  vatRate: string,
  original?: OriginalRetailPrice,
): number | null {
  const net = validMoney(netPrice);
  const vat = validVat(vatRate);
  if (net === null || vat === null) return null;
  if (original && netPrice === original.net && vatRate === original.vat) {
    return original.gross;
  }
  const netCents = Math.round(net * 100);
  return Math.round(netCents * (100 + vat) / 100) / 100;
}

export function preVatPriceFromGross(grossPrice: number, vatRate: number): string {
  const grossCents = Math.round(grossPrice * 100);
  const netCents = Math.round(grossCents * 100 / (100 + vatRate));
  return String(netCents / 100);
}
