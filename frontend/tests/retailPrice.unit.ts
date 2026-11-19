import assert from 'node:assert/strict';
import test from 'node:test';
import { calculateStoredSellPrice, preVatPriceFromGross } from '../src/lib/retailPrice.ts';

test('new product price includes VAT exactly once', () => {
  assert.equal(calculateStoredSellPrice('100000', '10'), 110000);
  assert.equal(calculateStoredSellPrice('100000', '0'), 100000);
  assert.equal(calculateStoredSellPrice('1000.25', '8'), 1080.27);
});

test('editing an existing product starts from its VAT-exclusive price', () => {
  const net = preVatPriceFromGross(110000, 10);
  assert.equal(net, '100000');
  assert.equal(calculateStoredSellPrice(net, '10'), 110000);
  assert.equal(calculateStoredSellPrice(net, '8'), 108000);
});

test('editing other fields preserves an existing gross price exactly', () => {
  const original = { gross: 0.05, net: preVatPriceFromGross(0.05, 10), vat: '10' };
  assert.equal(calculateStoredSellPrice(original.net, original.vat, original), 0.05);
  assert.equal(calculateStoredSellPrice(original.net, '20', original), 0.06);
});

test('invalid prices and VAT rates cannot be saved', () => {
  assert.equal(calculateStoredSellPrice('', '10'), null);
  assert.equal(calculateStoredSellPrice('-1', '10'), null);
  assert.equal(calculateStoredSellPrice('100', ''), null);
  assert.equal(calculateStoredSellPrice('100', '101'), null);
  assert.equal(calculateStoredSellPrice('100', '-1'), null);
});
