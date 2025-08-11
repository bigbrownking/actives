SELECT *
FROM (
select
 IIN_SELLER as iin_bin,
    IIN_CUSTOMER as iin_bin_pokup,
    IIN_SELLER as iin_bin_prod,
    toDate(INVOICE_DATE) as date,
    concat('ESF') as database,
    concat('Прочие активы') as aktivy,
    concat('Приобретение') as oper,
    concat('Турпакеты; ','Описание:',lowerUTF8(DESCRIPTION)) as dopinfo,
    concat('') as num_doc,
    toInt32(SUMMA_PRIOBRETENIE_V_TENGE) as summ
from pfr_dashboard.traveltur_esf_2023_24
) AS subquery
WHERE iin_bin = '$P-IIN'
AND date BETWEEN '$P-DATEFROM' AND '$P-DATETO';