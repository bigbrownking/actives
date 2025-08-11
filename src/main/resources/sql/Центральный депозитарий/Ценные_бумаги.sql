SELECT *
FROM (
select
    distinct
    iin_bin_derjatelya_akciy as iin_bin,
    toDate('2024-01-01') as date,
    concat('Центральный депозитарий') as database,
    concat('Ценные бумаги') as aktivy,
    concat('Наличие') as oper,
    concat('Намиенование акции: ',naimenovanye_emitenta,'; Кол-во акции:', "kolicestvo_obyavlennyh_akciy_31.12.2024",';') as dopinfo,
    toInt32(0) as summ



from pfr_dashboard.depositariy where iin_bin_derjatelya_akciy is not null
) AS subquery
WHERE iin_bin = '$P-IIN'
AND date BETWEEN '$P-DATEFROM' AND '$P-DATETO';