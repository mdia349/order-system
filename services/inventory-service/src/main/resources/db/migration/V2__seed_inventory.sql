INSERT INTO inventory_item(sku, quantity)
values ('SKU-CHAIR-1', 10)
on conflict (sku) do nothing;