SELECT
    iin_bin,
    iin_bin_pokup,
    iin_bin_prod,
    date,
    database,
    aktivy,
    oper,
    dopinfo,
    num_doc,
    summ
FROM (
    -- Query 1: Наличие from activy_po_crypte
   select
	iin_bin,
    iin_bin as iin_bin_pokup,
    concat('---') as iin_bin_prod,
    date,
    concat('ЕИАС') as database,
    concat('Цифровые активы') as aktivy,
    concat('Наличие') as oper,
    dop_info as dopinfo,
    concat('') as num_doc,
    abs(toInt32(toFloat64(summa))) as summ
from pfr_dashboard.activy_po_crypte

    UNION ALL

    -- Combined Query 2 and 3: Реализация and Приобретение from asloy_joined_table
    select
	CUSTOMER_MAINCODE as  iin_bin,
    SELLER_MAINCODE as iin_bin_pokup,
    CUSTOMER_MAINCODE as iin_bin_prod,
    toDate(DATE_OPER) as date,
    concat('ЕИАС') as database,
    concat('Цифровые активы') as aktivy,
    concat('Реализация') as oper,
    concat(coalesce(CFM_TYPE,''),', ',coalesce(CFM_NAME, ''),', ',dopinfo,'; Продовец: ', SELLER_MAINCODE,';') as dopinfo,
    concat('') as num_doc,
    toInt64(OPER_TENGE_AMOUNT as int) as summ
from (


select distinct
    MESS_ID,
    OPER_IDVIEW,
    OPER_TENGE_AMOUNT,
    OPER_NUMBER,
    DATE_OPER,
   	SELLER_MAINCODE,
   	dopinfo,
    CUSTOMER_MAINCODE,
    CFM_TYPE,
    CFM_NAME

from pfr_dashboard.asloy_joined_table where  (OPER_SUSP = '1119' or
OPER_SUSP = '1120' or
OPER_SUSP = '1121' or
OPER_SUSP = '1122' or
OPER_SUSP = '1123' or
OPER_SUSP = '1124' or
OPER_SUSP = '1125' or
OPER_SUSP = '1126' or
OPER_SUSP = '1127' or
OPER_SUSP = '1128' or
match(lowerUTF8(dopinfo),'крипт|виртуал|цифров.*валют|цифров.*актив') )
and not(MESS_REASON_CODE='12' or MESS_REASON_CODE='13' or MESS_REASON_CODE='14' or MESS_OPER_STATUS_CODE='2' or MESS_OPER_STATUS_CODE='3') and CUSTOMER_MAINCODE is not null and CUSTOMER_MAINCODE!='')




union all

select
	SELLER_MAINCODE as iin_bin,
    SELLER_MAINCODE as iin_bin_pokup,
    CUSTOMER_MAINCODE as iin_bin_prod,
    toDate(DATE_OPER) as date,
    concat('ЕИАС') as database,
    concat('Цифровые активы') as aktivy,
    concat('Приобретение') as oper,
    concat(coalesce(CFM_TYPE,''),', ',coalesce(CFM_NAME, ''),', ',dopinfo,'; Покупатель: ', CUSTOMER_MAINCODE,';') as dopinfo,
    concat('') as num_doc,
    toInt64(OPER_TENGE_AMOUNT as int) as summ
from (


select distinct
    MESS_ID,
    OPER_IDVIEW,
    OPER_TENGE_AMOUNT,
    OPER_NUMBER,
    DATE_OPER,
   CUSTOMER_MAINCODE,
    SELLER_MAINCODE,
    dopinfo,
    CFM_TYPE,
    CFM_NAME

from pfr_dashboard.asloy_joined_table where  (OPER_SUSP = '1119' or
OPER_SUSP = '1120' or
OPER_SUSP = '1121' or
OPER_SUSP = '1122' or
OPER_SUSP = '1123' or
OPER_SUSP = '1124' or
OPER_SUSP = '1125' or
OPER_SUSP = '1126' or
OPER_SUSP = '1127' or
OPER_SUSP = '1128' or
match(lowerUTF8(dopinfo),'крипт|виртуал|цифров.*валют|цифров.*актив') )
and not(MESS_REASON_CODE='12' or MESS_REASON_CODE='13' or MESS_REASON_CODE='14' or MESS_OPER_STATUS_CODE='2' or MESS_OPER_STATUS_CODE='3') and SELLER_MAINCODE is not null and SELLER_MAINCODE!='')

) AS combined
where iin_bin = '$P-IIN';
AND date BETWEEN '$P-DATEFROM' AND '$P-DATETO';