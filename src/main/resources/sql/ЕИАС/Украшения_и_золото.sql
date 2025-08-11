SELECT *
FROM (
select
	CUSTOMER_MAINCODE as iin_bin,
    SELLER_MAINCODE as iin_bin_pokup,
    CUSTOMER_MAINCODE as iin_bin_prod,
    toDate(DATE_OPER) as date,
    concat('ЕИАС') as database,
    concat('Украшения') as aktivy,
    concat('Реализация') as oper,
    concat(coalesce(CFM_TYPE,''),', ',coalesce(CFM_NAME, ''),', ',dopinfo,'; Продовец: ', SELLER_MAINCODE,';') as dopinfo,
    concat('') as num_doc,
    toInt64(OPER_TENGE_AMOUNT as int) as summ

from (



select
    distinct
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
from pfr_dashboard.asloy_joined_table where



(OPER_IDVIEW='1011' or OPER_IDVIEW='1012' or OPER_IDVIEW='1021' or OPER_IDVIEW='1022' or OPER_IDVIEW='1711' or OPER_IDVIEW='1721' or OPER_SUSP='1089' or OPER_SUSP='1090'  or
(

(
      match(lowerUTF8(dopinfo), 'часы.*(rolex|omega|tag heuer|audemars piguet|seiko|casio|swatch|longines|breitling|hublot|iwc schaffhausen)')
      OR match(lowerUTF8(dopinfo),
          'золот.*(браслет|часы|серьги|колье|ожерелье|диадема|наручные часы|подвеска|изделие|изделия)|' ||
          'бриллиант.*(браслет|часы|серьги|колье|ожерелье|диадема|наручные часы|подвеска|изделие|изделия)|' ||
          'жемчуг.*(браслет|часы|серьги|колье|ожерелье|диадема|наручные часы|подвеска|изделие|изделия)|' ||
          'изумруд.*(браслет|часы|серьги|колье|ожерелье|диадема|наручные часы|подвеска|изделие|изделия)|' ||
          'проба.*(браслет|часы|серьги|колье|ожерелье|диадема|наручные часы|подвеска|изделие|изделия)|' ||
          'пробы.*(браслет|часы|серьги|колье|ожерелье|диадема|наручные часы|подвеска|изделие|изделия)|' ||
          'драгоцен.*(браслет|часы|серьги|колье|ожерелье|диадема|наручные часы|подвеска|изделие|изделия)|' ||
          'карат.*(браслет|часы|серьги|колье|ожерелье|диадема|наручные часы|подвеска|изделие|изделия)|' ||
          'алмаз.*(браслет|часы|серьги|колье|ожерелье|диадема|наручные часы|подвеска|изделие|изделия)|' ||
          'рубин.*(браслет|часы|серьги|колье|ожерелье|диадема|наручные часы|подвеска|изделие|изделия)|' ||
          'сапфир.*(браслет|часы|серьги|колье|ожерелье|диадема|наручные часы|подвеска|изделие|изделия)')
       OR match(lowerUTF8(dopinfo), 'наручные.*часы|ювелирные.*часы|часы.*женские|часы.*мужские|смарт.*часы|часы.*механ.*автомат|часы.*кварц|ювелирные.*изделия|ювелирное.*изделие|бриллиант.*кольц|золот.*желт.*кольц|золот.*бел.*кольц|золот.*розов.*кольц|изумруд.*кольц|жемчуг.*кольц|кольц.*карат|драгоцен.*кольц|алмаз.*кольц|бриллиант.*камень|бриллиант.*камн|золот.*медаль|золот.*часы|часы.*браслет|камин.*часы|напольн.*часы')

)


      AND NOT match(lowerUTF8(dopinfo), 'работ|отход|переработк|услуг|перевозк|аренд|охран|оборот|анализ|определен|дефект|коробк|учет|лом|недраг|гипс|бижутер|ремонт|реставр')

))
and not(MESS_REASON_CODE='12' or MESS_REASON_CODE='13' or MESS_REASON_CODE='14' or MESS_OPER_STATUS_CODE='2' or MESS_OPER_STATUS_CODE='3')  and CUSTOMER_MAINCODE is not null and CUSTOMER_MAINCODE!=''
)

union all

select
	CUSTOMER_MAINCODE as iin_bin,
    SELLER_MAINCODE as iin_bin_pokup,
    CUSTOMER_MAINCODE as iin_bin_prod,
    toDate(DATE_OPER) as date,
    concat('ЕИАС') as database,
    concat('Золото') as aktivy,
    concat('Реализация') as oper,
    concat(coalesce(CFM_TYPE,''),', ',coalesce(CFM_NAME, ''),', ',dopinfo,'; Продовец: ', SELLER_MAINCODE,';') as dopinfo,
    concat('') as num_doc,
    toInt64(OPER_TENGE_AMOUNT as int) as summ

from (



select
    distinct
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
from pfr_dashboard.asloy_joined_table where



((OPER_IDVIEW='1711' or OPER_IDVIEW='1721')  and
(

(
      match(lowerUTF8(dopinfo), 'золот.*слиток|золот.*слитках|золот.*аффинирован|золот.*катод|инвест.*золот|золот.*проба|золот.*пробы|золот.*585|золот.*монета|золото.*гранул')
)


      AND NOT match(lowerUTF8(dopinfo), 'работ|отход|переработк|услуг|перевозк|аренд|охран|оборот|анализ|определен|дефект|коробк|учет|лом|недраг|гипс|бижутер|ремонт|реставр')

))
and not(MESS_OPER_STATUS_CODE='2')  and CUSTOMER_MAINCODE is not null and CUSTOMER_MAINCODE!='')

) AS subquery
WHERE iin_bin = '$P-IIN'
AND date BETWEEN '$P-DATEFROM' AND '$P-DATETO';
