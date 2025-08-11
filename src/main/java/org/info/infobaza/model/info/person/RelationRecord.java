package org.info.infobaza.model.info.person;

import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class RelationRecord {
    private String iin_1;
    private String iin_2;
    private String vid_sviazi;
    private String status;
    private int level_rod;

    public RelationRecord(String iin_1, String iin_2, String vid_sviazi, String status) {
        this.iin_1 = iin_1;
        this.iin_2 = iin_2;
        this.vid_sviazi = vid_sviazi;
        this.status = status;
    }
}