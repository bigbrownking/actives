SELECT *
FROM (
select
    iin_bin,
    '' as iin_bin_pokup,
    '' as iin_bin_prod,
    toDate('2024-01-01') as date,
    concat('FNO250') as database,
    concat('Транспортные средства') as aktivy,
    concat('Наличие') as oper,
    concat('Кол-во ТС:',kolichestv_transport_sredstv_zaregan_v_inos_gos,'; Наименование и коды стран:', vse_marki_modely_transport_credstv_i_kod_stran,';') as dopinfo,
    concat('') as num_doc,
    toInt32(0) as summ

from pfr_dashboard.fno_250_2023_24 where vse_marki_modely_transport_credstv_i_kod_stran is not null
) AS subquery
WHERE iin_bin = '$P-IIN'
AND date BETWEEN '$P-DATEFROM' AND '$P-DATETO';