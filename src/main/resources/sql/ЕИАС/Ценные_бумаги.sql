SELECT *
FROM (
select
	CUSTOMER_MAINCODE as iin_bin,
    CUSTOMER_MAINCODE as iin_bin_prod,
    SELLER_MAINCODE as iin_bin_pokup,
    toDate(DATE_OPER) as date,
    concat('ЕИАС') as database,
    concat('Ценные бумаги') as aktivy,
    concat('Реализация') as oper,
    concat(coalesce(CFM_TYPE,''),', ',coalesce(CFM_NAME, ''),', ',dopinfo,'; Продовец: ', SELLER_MAINCODE,';') as dopinfo,
    concat('') as num_doc,
    toInt64(OPER_TENGE_AMOUNT) as summ
from (

select distinct
    MESS_ID,
    OPER_IDVIEW,
    OPER_TENGE_AMOUNT,
    OPER_NUMBER,
    DATE_OPER,
    dopinfo,
    SELLER_MAINCODE,
    CUSTOMER_MAINCODE,
    CFM_TYPE,
    CFM_NAME

from pfr_dashboard.asloy_joined_table where (OPER_IDVIEW='1212' or OPER_IDVIEW='1222' or OPER_IDVIEW='1911' or OPER_IDVIEW='2020' or OPER_IDVIEW='2110' or OPER_SUSP='1099' or OPER_SUSP='1074' or OPER_SUSP='1075' or OPER_SUSP='1076' or OPER_SUSP='1102' or OPER_SUSP='1130' or match(lowerUTF8(dopinfo), 'облиц|вексел|инвест.*(пай|паев|паи)|ценн.*бумаг'))
and not(MESS_REASON_CODE='12' or MESS_REASON_CODE='13' or MESS_REASON_CODE='14' or MESS_OPER_STATUS_CODE='2' or MESS_OPER_STATUS_CODE='3') and SELLER_MAINCODE is not null and SELLER_MAINCODE!='')


union all

select
	SELLER_MAINCODE as iin_bin,
    CUSTOMER_MAINCODE as iin_bin_prod,
    SELLER_MAINCODE as iin_bin_pokup,
    toDate(DATE_OPER) as date,
    concat('ЕИАС') as database,
    concat('Ценные бумаги') as aktivy,
    concat('Приобретение') as oper,
    concat(coalesce(CFM_TYPE,''),', ',coalesce(CFM_NAME, ''),', ',dopinfo,'; Покупатель: ', CUSTOMER_MAINCODE,';') as dopinfo,
    concat('') as num_doc,
    toInt64(OPER_TENGE_AMOUNT) as summ
from (

select distinct
    MESS_ID,
    OPER_IDVIEW,
    OPER_TENGE_AMOUNT,
    OPER_NUMBER,
    DATE_OPER,
    SELLER_MAINCODE,
    CUSTOMER_MAINCODE,
    dopinfo,
    CFM_TYPE,
    CFM_NAME

from pfr_dashboard.asloy_joined_table where (OPER_IDVIEW='1212' or OPER_IDVIEW='1222' or OPER_IDVIEW='1911' or OPER_IDVIEW='2020' or OPER_IDVIEW='2110' or OPER_SUSP='1099' or OPER_SUSP='1074' or OPER_SUSP='1075' or OPER_SUSP='1076' or OPER_SUSP='1102' or OPER_SUSP='1130' or match(lowerUTF8(dopinfo), 'облиц|вексел|инвест.*(пай|паев|паи)|ценн.*бумаг'))
and not(MESS_REASON_CODE='12' or MESS_REASON_CODE='13' or MESS_REASON_CODE='14' or MESS_OPER_STATUS_CODE='2' or MESS_OPER_STATUS_CODE='3') and CUSTOMER_MAINCODE is not null and CUSTOMER_MAINCODE!='')

) AS subquery
WHERE iin_bin = '$P-IIN'
AND date BETWEEN '$P-DATEFROM' AND '$P-DATETO';