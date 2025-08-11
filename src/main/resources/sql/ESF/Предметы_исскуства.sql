select *
from(
select
	IIN_SELLER as iin_bin,
    IIN_CUSTOMER as iin_bin_pokup,
    IIN_SELLER as iin_bin_prod,
    toDate(INVOICE_DATE) as date,
    concat('ESF') as database,
    concat('Предметы исскуства') as aktivy,
    concat('Реализация') as oper,
    concat('Предметы исскуства; ','Описание:',lowerUTF8(DESCRIPTION),'; Покупатель:', IIN_CUSTOMER,';') as dopinfo,
    concat('') as num_doc,
    toFloat64(TURNOVER_SIZE) as summ
from pfr_dashboard.act_esf_iskustvo

union all

select
	IIN_CUSTOMER as iin_bin,
    IIN_CUSTOMER as iin_bin_pokup,
    IIN_SELLER as iin_bin_prod,
    toDate(INVOICE_DATE) as date,
    concat('ESF') as database,
    concat('Предметы исскуства') as aktivy,
    concat('Приобретение') as oper,
    concat('Предметы исскуства; ','Описание:',lowerUTF8(DESCRIPTION),'; Продовец: ', IIN_SELLER,';') as dopinfo,
    concat('') as num_doc,
    toFloat64(TURNOVER_SIZE) as summ
from pfr_dashboard.act_esf_iskustvo

) as subquery
where iin_bin = '$P-IIN'
AND date BETWEEN '$P-DATEFROM' AND '$P-DATETO';
