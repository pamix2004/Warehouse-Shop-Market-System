package com.warehouseManagement.demo;

import com.warehouseManagement.demo.dto.OfferViewDTO;
import com.warehouseManagement.demo.repo.*;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;


import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/offers")
public class OfferPublicRestController {

    @Autowired
    private OfferRepository offerRepository;

    @GetMapping("/public")
    public List<OfferViewDTO> getOffersForStores() {
        return offerRepository.findAll().stream()
                .filter(o -> o.getAvailable_quantity() > 0)
                .map(o -> {
                    OfferViewDTO dto = new OfferViewDTO();
                    dto.setId(o.getId());
                    dto.setProductName(o.getProduct().getName());
                    dto.setPrice(o.getPrice());
                    dto.setAvailableQuantity(o.getAvailable_quantity());
                    dto.setWholesalerName(o.getWholesaler().getName());
                    return dto;
                })
                .toList();
    }
}
