package org.info.infobaza.model.dossierprime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "photo", schema = "import_fl")
@Getter
@Setter
public class PhotoDb {

    private String iin;

    private String document_type_id;

    private byte[] photo;
    @Id
    private String date;
    @Override
    public String toString() {
        return "photoDb{" +
                "iin='" + iin + '\'' +
                ", document_type_id='" + document_type_id + '\'' +
                ", date='" + date + '\'' +
                '}';
    }
}
