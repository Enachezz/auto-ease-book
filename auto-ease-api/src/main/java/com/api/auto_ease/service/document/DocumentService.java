package com.api.auto_ease.service.document;

import com.api.auto_ease.adapter.DocumentAdapter;
import com.api.auto_ease.domain.car.Car;
import com.api.auto_ease.domain.serviceEntry.ServiceEntry;
import com.api.auto_ease.repository.car.CarRepository;
import com.api.auto_ease.repository.serviceEntry.ServiceEntryRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Service
@Slf4j
public class DocumentService {

    private CarRepository carRepository;
    private ServiceEntryRepository serviceEntryRepository;
    private DocumentAdapter documentAdapter;

    public String generateCarServiceHistory(String vinNumber) {

        DocumentDTO documentDTO = new DocumentDTO();

        Car car = carRepository.findByVin(vinNumber);
        if(car == null) {
            throw new RuntimeException("No car found for this VIN");
        }

        List<ServiceEntry> serviceEntryList = serviceEntryRepository.findAllByCarVin(vinNumber);
        List<DocumentServiceEntryDTO> documentServiceEntryDTOList = new ArrayList<>();

        for(ServiceEntry serviceEntry : serviceEntryList) {
            documentServiceEntryDTOList.add(
                    new DocumentServiceEntryDTO(serviceEntry.getServiceDate(),
                            serviceEntry.getEntryType(),
                            serviceEntry.getDescription()));
        }

        documentDTO.setVin(vinNumber);
        documentDTO.setEntries(documentServiceEntryDTOList);

        return documentAdapter.doCallToApi(documentDTO);
    }
}
