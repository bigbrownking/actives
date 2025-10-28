package org.info.infobaza.dto.response.info.car;

import lombok.Builder;
import lombok.Data;
import org.info.infobaza.model.info.object.Car;
import org.info.infobaza.model.info.object.CarInsurance;
import org.info.infobaza.model.info.object.CarInsuranceSummary;

import java.util.List;

@Data
@Builder
public class CarPortret implements CarResponse{
   private List<CarPortretPiece> portrets;
}
