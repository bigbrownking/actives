SELECT *
FROM (
    SELECT
        SELLER_MAINCODE AS iin_bin,
        SELLER_MAINCODE AS iin_bin_pokup,
        CUSTOMER_MAINCODE AS iin_bin_prod,
        toDate(DATE_OPER) AS date,
        concat('ЕИАС') AS database,
        concat('Недвижимое имущество') AS aktivy,
        concat('Приобретение') AS oper,
        concat(coalesce(CFM_TYPE,''),', ',coalesce(CFM_NAME, ''),', ',dopinfo,'; Покупатель: ', CUSTOMER_MAINCODE,';') AS dopinfo,
        concat('') AS num_doc,
        toInt64(OPER_TENGE_AMOUNT) AS summ
    FROM (
        SELECT DISTINCT
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
        FROM pfr_dashboard.asloy_joined_table
        WHERE (OPER_SUSP='1068' OR OPER_SUSP='1092' OR OPER_SUSP='1093' OR OPER_SUSP='1130' OR OPER_SUSP='1131' OR OPER_SUSP='9014')
        AND NOT (MESS_REASON_CODE='12' OR MESS_REASON_CODE='13' OR MESS_REASON_CODE='14' OR MESS_OPER_STATUS_CODE='2' OR MESS_OPER_STATUS_CODE='3')
        AND SELLER_MAINCODE IS NOT NULL AND SELLER_MAINCODE!=''
        AND DATE_OPER BETWEEN toDate('$P-DATEFROM') AND toDate('$P-DATETO')

        UNION ALL

        SELECT DISTINCT
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
        FROM pfr_dashboard.asloy_joined_table
        WHERE (OPER_IDVIEW='1811' OR OPER_SUSP='1092' OR OPER_SUSP='9014' OR OPER_IDTYPE='721' OR OPER_IDTYPE='722')
        AND NOT (MESS_REASON_CODE='12' OR MESS_REASON_CODE='13' OR MESS_REASON_CODE='14' OR MESS_OPER_STATUS_CODE='2' OR MESS_OPER_STATUS_CODE='3')
        AND SELLER_MAINCODE IS NOT NULL AND SELLER_MAINCODE!=''
        AND DATE_OPER BETWEEN toDate('$P-DATEFROM') AND toDate('$P-DATETO')
    ) AS purchase_data

    UNION ALL

    SELECT
        CUSTOMER_MAINCODE AS iin_bin,
        SELLER_MAINCODE AS iin_bin_pokup,
        CUSTOMER_MAINCODE AS iin_bin_prod,
        toDate(DATE_OPER) AS date,
        concat('ЕИАС') AS database,
        concat('Недвижимое имущество') AS aktivy,
        concat('Реализация') AS oper,
        concat(coalesce(CFM_TYPE,''),', ',coalesce(CFM_NAME, ''),', ',dopinfo,'; Продовец: ', SELLER_MAINCODE,';') AS dopinfo,
        concat('') AS num_doc,
        toInt64(OPER_TENGE_AMOUNT) AS summ
    FROM (
        SELECT DISTINCT
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
        FROM pfr_dashboard.asloy_joined_table
        WHERE (OPER_SUSP='1068' OR OPER_SUSP='1092' OR OPER_SUSP='1093' OR OPER_SUSP='1130' OR OPER_SUSP='1131' OR OPER_SUSP='9014')
        AND NOT (MESS_REASON_CODE='12' OR MESS_REASON_CODE='13' OR MESS_REASON_CODE='14' OR MESS_OPER_STATUS_CODE='2' OR MESS_OPER_STATUS_CODE='3')
        AND CUSTOMER_MAINCODE IS NOT NULL AND CUSTOMER_MAINCODE!=''
        AND DATE_OPER BETWEEN toDate('$P-DATEFROM') AND toDate('$P-DATETO')

        UNION ALL

        SELECT DISTINCT
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
        FROM pfr_dashboard.asloy_joined_table
        WHERE (OPER_IDVIEW='1811' OR OPER_SUSP='1092' OR OPER_SUSP='9014' OR OPER_IDTYPE='721' OR OPER_IDTYPE='722')
        AND NOT (MESS_REASON_CODE='12' OR MESS_REASON_CODE='13' OR MESS_REASON_CODE='14' OR MESS_OPER_STATUS_CODE='2' OR MESS_OPER_STATUS_CODE='3')
        AND CUSTOMER_MAINCODE IS NOT NULL AND CUSTOMER_MAINCODE!=''
        AND DATE_OPER BETWEEN toDate('$P-DATEFROM') AND toDate('$P-DATETO')
    ) AS sale_data
) AS combined_data
WHERE iin_bin = '$P-IIN';