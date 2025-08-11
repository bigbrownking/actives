select *
from(
select
	IIN_CUSTOMER as iin_bin,
    IIN_CUSTOMER as iin_bin_pokup,
    concat('---') as iin_bin_prod,
    toDate(INVOICE_DATE) as date,
    concat('ESF') as database,
    concat('Сейфовые ячейки') as aktivy,
    concat('Наличие') as oper,
    concat('Описание:',lowerUTF8(DESCRIPTION)) as dopinfo,
    concat('') as num_doc,
    abs(toInt64(SUMMA_PRIOBRETENIE_V_TENGE)) as summ
from pfr_dashboard.seif_esf_2023_24)
 as subquery
where iin_bin = '$P-IIN'
AND date BETWEEN '$P-DATEFROM' AND '$P-DATETO';
