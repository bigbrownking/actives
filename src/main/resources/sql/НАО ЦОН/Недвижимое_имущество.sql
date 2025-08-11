select * from pfr_dashboard.active_table_2_1_tb
where iin_bin = '$P-IIN'
AND date BETWEEN '$P-DATEFROM' AND '$P-DATETO';