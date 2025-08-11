SELECT *
FROM (
SELECT
    iin AS iin_bin,
    date,
    CONCAT('ЕНИС НОТАРИУС') AS database,
    CASE
        WHEN code = '0' THEN 'Иные имущества'
        WHEN code = '2' THEN 'Земельный участок'
        WHEN code = '3' THEN 'Недвижимое имущество'
        WHEN code = '4' THEN 'Недвижимое имущество'
        WHEN code = '5' THEN 'Недвижимое имущество'
        WHEN code = '6' THEN 'Транспортные средства'
        WHEN code = '8' THEN 'Иные имущества'
        WHEN code = '29' THEN 'Недвижимое имущество'
        WHEN code = '30' THEN 'Денежные средства'
        WHEN code = '31' THEN 'Денежные средства'
        WHEN code = '32' THEN 'Денежные средства'
        WHEN code = '33' THEN 'Договор подряда'
        WHEN code = '34' THEN 'Иные имущества'
        WHEN code = '35' THEN 'Иные имущества'
        WHEN code = '36' THEN 'Иные имущества'
        WHEN code = '37' THEN 'Иные имущества'
        WHEN code = '38' THEN 'Доли в уставном капитале'
        WHEN code = '39' THEN 'Иные имущества'
        WHEN code = '40' THEN 'Денежные средства'
        WHEN code = '54' THEN 'Денежные средства'
        WHEN code = '56' THEN 'Иные имущества'
        WHEN code = '60' THEN 'Доли в уставном капитале'
        WHEN code = '61' THEN 'Иные имущества'
        ELSE '0'
    END AS aktivy,
    CONCAT('Приобретение') AS oper,
    CONCAT('Сумма сделки:', amount, '; ИИН нотариуса:', notary_iin, '; Документ:', replaceRegexpAll(lowerUTF8(subject), '\\s+', ' '), ';') AS dopinfo,
    COALESCE(amount, '0') AS summ
FROM pfr_dashboard.enisprod
WHERE (iin <> iin_2 OR iin IS NOT NULL)
    AND NOT (code IN ('1', '12', '14', '15', '16', '17', '18', '20', '21', '22', '24', '26', '27', '41', '42', '47', '48', '49', '50', '51', '59'))


UNION ALL

SELECT
    iin_2 AS iin_bin,
    date,
    CONCAT('ЕНИС НОТАРИУС') AS database,
    CASE
        WHEN code = '0' THEN 'Иные имущества'
        WHEN code = '2' THEN 'Земельный участок'
        WHEN code = '3' THEN 'Недвижимое имущество'
        WHEN code = '4' THEN 'Недвижимое имущество'
        WHEN code = '5' THEN 'Недвижимое имущество'
        WHEN code = '6' THEN 'Транспортные средства'
        WHEN code = '8' THEN 'Иные имущества'
        WHEN code = '29' THEN 'Недвижимое имущество'
        WHEN code = '30' THEN 'Денежные средства'
        WHEN code = '31' THEN 'Денежные средства'
        WHEN code = '32' THEN 'Денежные средства'
        WHEN code = '33' THEN 'Договор подряда'
        WHEN code = '34' THEN 'Иные имущества'
        WHEN code = '35' THEN 'Иные имущества'
        WHEN code = '36' THEN 'Иные имущества'
        WHEN code = '37' THEN 'Иные имущества'
        WHEN code = '38' THEN 'Доли в уставном капитале'
        WHEN code = '39' THEN 'Иные имущества'
        WHEN code = '40' THEN 'Денежные средства'
        WHEN code = '54' THEN 'Денежные средства'
        WHEN code = '56' THEN 'Иные имущества'
        WHEN code = '60' THEN 'Доли в уставном капитале'
        WHEN code = '61' THEN 'Иные имущества'
        ELSE '0'
    END AS aktivy,
    CONCAT('Реализация') AS oper,
    CONCAT('Сумма сделки:', amount, '; ИИН нотариуса:', notary_iin, '; Документ:', replaceRegexpAll(lowerUTF8(subject), '\\s+', ' '), ';') AS dopinfo,
    COALESCE(amount, '0') AS summ
FROM pfr_dashboard.enisprod
WHERE (iin <> iin_2 OR iin IS NOT NULL)
    AND NOT (code IN ('1', '12', '14', '15', '16', '17', '18', '20', '21', '22', '24', '26', '27', '41', '42', '47', '48', '49', '50', '51', '59'))
) AS subquery
WHERE iin_bin = '$P-IIN'
AND date BETWEEN '$P-DATEFROM' AND '$P-DATETO';