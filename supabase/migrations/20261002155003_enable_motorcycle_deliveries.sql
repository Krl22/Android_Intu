-- Courier ordering is available with the completed 1.23 client and verified PIN/payment workflow.
-- Vehicle approval is still mandatory; passenger and delivery matching remain separate.
update public.vehicle_types set is_active = true where code = 'motorcycle' and service_kind = 'delivery';
