begin;
-- This suite validates the opt-in PIN flow, without changing the live preference.
update private.ride_security_settings set pin_enabled = true where singleton;
do $$ begin
  if exists(select 1 from public.profiles where id like 'intu-qa-delivery-20261002-%') then raise exception 'Fixture collision'; end if;
  if has_function_privilege('anon', 'public.confirm_delivery_payment(uuid)', 'execute') then raise exception 'Anonymous payment RPC'; end if;
end $$;
insert into public.profiles(id, first_name, last_name)
  select 'intu-qa-delivery-20261002-' || name, 'QA', name
  from unnest(array['sender1','sender2','courier1','courier2','taxi']) name;
insert into public.drivers(id,document_type,document_number,license_number,status)
values ('intu-qa-delivery-20261002-courier1','dni','99161101','QA','approved'),
       ('intu-qa-delivery-20261002-courier2','dni','99161102','QA','approved'),
       ('intu-qa-delivery-20261002-taxi','dni','99161103','QA','approved');
insert into public.vehicles(driver_id,vehicle_type,brand,model,year,plate)
values ('intu-qa-delivery-20261002-courier1','motorcycle','QA','QA',2024,'QADC101'),
       ('intu-qa-delivery-20261002-courier2','motorcycle','QA','QA',2024,'QADC102'),
       ('intu-qa-delivery-20261002-taxi','mototaxi','QA','QA',2024,'QADT101');
-- Activate only inside this rolled-back test until the finished courier APK is delivered.
update public.vehicle_types set is_active = true where code = 'motorcycle';
set local role authenticated;
select set_config('request.jwt.claims','{"sub":"intu-qa-delivery-20261002-sender1","role":"authenticated"}',true);
do $$ begin
  begin
    perform public.create_delivery_request(-11.252,-74.637,'QA',-11.25,-74.63,'QA',0,0,null,'efectivo',
      '{"recipient_name":"Persona QA","recipient_phone":"+51987654321","description":"Sobre QA","payer":"sender","small_package_confirmed":false}');
    raise exception 'Unacknowledged parcel accepted';
  exception when invalid_parameter_value then null;
  end;
  begin
    perform public.create_delivery_request(-11.252,-74.637,'QA',-11.25,-74.63,'QA',0,0,null,'efectivo',
      '{"recipient_name":"Persona QA","recipient_phone":"+51987654321","description":"Sobre QA","payer":"sender","small_package_confirmed":"true"}');
    raise exception 'String acknowledgement accepted';
  exception when invalid_parameter_value then null;
  end;
  begin
    perform public.create_delivery_request(-11.252,-74.637,'QA Origen',-11.25,-74.63,'QA Destino',2100,420,null,'efectivo',
      '{"recipient_name":"Persona QA","recipient_phone":"987","description":"Sobre QA","payer":"recipient","small_package_confirmed":true}');
    raise exception 'Invalid recipient phone accepted';
  exception when check_violation then null;
  end;
  if exists(select 1 from public.rides) then raise exception 'Invalid details left an orphan order'; end if;
  begin
    insert into public.rides(vehicle_type,origin_lat,origin_lng,origin_address,destination_lat,destination_lng,destination_address,distance_meters,duration_seconds)
    values ('motorcycle',-11.252,-74.637,'QA',-11.25,-74.63,'QA',2100,420);
    set constraints rides_require_delivery_details immediate;
    raise exception 'Direct INSERT bypassed delivery detail requirement';
  exception when check_violation then null;
  end;
end $$;
select set_config('intu.qa_delivery1', (public.create_delivery_request(-11.252,-74.637,'QA Origen',-11.25,-74.63,'QA Destino',2100,420,null,'efectivo',
  '{"recipient_name":"Persona QA","recipient_phone":"+51987654321","description":"Sobre QA","payer":"recipient","small_package_confirmed":true}')).id::text, true);
select set_config('intu.qa_pin1',(select pin from public.ride_start_pin(current_setting('intu.qa_delivery1')::uuid)),true);
do $$ begin
  if not exists(select 1 from public.rides where id = current_setting('intu.qa_delivery1')::uuid
                  and estimated_fare = 4.20 and service_kind = 'delivery' and vehicle_type = 'motorcycle') then
    raise exception 'Delivery tariff/type incorrect';
  end if;
  if not exists(select 1 from public.delivery_details where ride_id = current_setting('intu.qa_delivery1')::uuid) then
    raise exception 'Sender cannot read delivery details';
  end if;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-delivery-20261002-taxi","role":"authenticated"}',true);
do $$ begin
  if exists(select 1 from public.rides where id = current_setting('intu.qa_delivery1')::uuid) then raise exception 'Mototaxi sees courier order'; end if;
  begin
    perform public.accept_ride(current_setting('intu.qa_delivery1')::uuid);
    raise exception 'Mototaxi accepted courier order';
  exception when no_data_found then null;
  end;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-delivery-20261002-courier1","role":"authenticated"}',true);
do $$ begin
  if not exists(select 1 from public.rides where id = current_setting('intu.qa_delivery1')::uuid) then raise exception 'Courier cannot see delivery offer'; end if;
  if exists(select 1 from public.delivery_details where ride_id = current_setting('intu.qa_delivery1')::uuid) then raise exception 'Unassigned courier sees contact'; end if;
end $$;
select public.accept_ride(current_setting('intu.qa_delivery1')::uuid);
do $$ begin
  if not exists(select 1 from public.delivery_details where ride_id = current_setting('intu.qa_delivery1')::uuid) then raise exception 'Assigned courier cannot read recipient'; end if;
  begin
    perform public.confirm_delivery_payment(current_setting('intu.qa_delivery1')::uuid);
    raise exception 'Recipient payment collected before delivery';
  exception when raise_exception then if sqlerrm <> 'invalid_payment_stage' then raise; end if;
  end;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-delivery-20261002-courier2","role":"authenticated"}',true);
do $$ begin
  if exists(select 1 from public.delivery_details where ride_id = current_setting('intu.qa_delivery1')::uuid) then raise exception 'Other courier sees recipient'; end if;
  begin
    perform public.confirm_delivery_payment(current_setting('intu.qa_delivery1')::uuid);
    raise exception 'Other courier records payment';
  exception when no_data_found then null;
  end;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-delivery-20261002-courier1","role":"authenticated"}',true);
select public.advance_ride(current_setting('intu.qa_delivery1')::uuid,'arrived');
select public.verify_ride_pin(current_setting('intu.qa_delivery1')::uuid,current_setting('intu.qa_pin1'));
select public.advance_ride(current_setting('intu.qa_delivery1')::uuid,'in_progress');
do $$ begin
  begin
    perform public.advance_ride(current_setting('intu.qa_delivery1')::uuid,'completed');
    raise exception 'Unpaid delivery completed';
  exception when raise_exception then if sqlerrm <> 'delivery_payment_required' then raise; end if;
  end;
end $$;
select public.confirm_delivery_payment(current_setting('intu.qa_delivery1')::uuid);
select public.confirm_delivery_payment(current_setting('intu.qa_delivery1')::uuid);
select public.advance_ride(current_setting('intu.qa_delivery1')::uuid,'completed');
do $$ begin
  if not exists(select 1 from public.rides where id = current_setting('intu.qa_delivery1')::uuid and status = 'completed'
                  and final_fare = 4.20 and payment_confirmed_at is not null) then raise exception 'Delivery completion failed'; end if;
end $$;
-- Sender-paid lifecycle: verified pickup PIN -> payment collection -> pickup -> delivery.
select set_config('request.jwt.claims','{"sub":"intu-qa-delivery-20261002-sender2","role":"authenticated"}',true);
select set_config('intu.qa_delivery2',(public.create_delivery_request(-11.252,-74.637,'QA Origen',-11.25,-74.63,'QA Destino',0,0,null,'yape_plin',
  '{"recipient_name":"Persona QA","recipient_phone":"+51987654321","description":"Paquete QA","payer":"sender","small_package_confirmed":true}')).id::text,true);
select set_config('intu.qa_pin2',(select pin from public.ride_start_pin(current_setting('intu.qa_delivery2')::uuid)),true);
select set_config('request.jwt.claims','{"sub":"intu-qa-delivery-20261002-courier1","role":"authenticated"}',true);
select public.accept_ride(current_setting('intu.qa_delivery2')::uuid);
select public.advance_ride(current_setting('intu.qa_delivery2')::uuid,'arrived');
do $$ begin
  begin
    perform public.confirm_delivery_payment(current_setting('intu.qa_delivery2')::uuid);
    raise exception 'Sender payment accepted without pickup PIN';
  exception when raise_exception then if sqlerrm <> 'pin_required' then raise; end if;
  end;
end $$;
select public.verify_ride_pin(current_setting('intu.qa_delivery2')::uuid,current_setting('intu.qa_pin2'));
do $$ begin
  begin
    perform public.advance_ride(current_setting('intu.qa_delivery2')::uuid,'in_progress');
    raise exception 'Sender-paid delivery picked up without receipt';
  exception when raise_exception then if sqlerrm <> 'delivery_payment_required' then raise; end if;
  end;
end $$;
select public.confirm_delivery_payment(current_setting('intu.qa_delivery2')::uuid);
select set_config('request.jwt.claims','{"sub":"intu-qa-delivery-20261002-sender2","role":"authenticated"}',true);
do $$ begin
  begin
    perform public.cancel_ride(current_setting('intu.qa_delivery2')::uuid,'QA');
    raise exception 'Sender cancelled a paid handover';
  exception when raise_exception then if sqlerrm <> 'delivery_in_custody' then raise; end if;
  end;
end $$;
select set_config('request.jwt.claims','{"sub":"intu-qa-delivery-20261002-courier1","role":"authenticated"}',true);
do $$ begin
  begin
    perform public.cancel_ride(current_setting('intu.qa_delivery2')::uuid,'QA');
    raise exception 'Paid pickup was rematched to another courier';
  exception when raise_exception then if sqlerrm <> 'delivery_in_custody' then raise; end if;
  end;
end $$;
select public.advance_ride(current_setting('intu.qa_delivery2')::uuid,'in_progress');
select public.advance_ride(current_setting('intu.qa_delivery2')::uuid,'completed');
set constraints all immediate;
reset role;
rollback;
select 'delivery lifecycle/privacy/atomic creation/PIN/payment stages/minimum/20% fare: PASS' as verification;
