package com.warehouseManagement.demo.services;

import com.warehouseManagement.demo.OfferState;
import com.warehouseManagement.demo.entity.Offer;
import com.warehouseManagement.demo.entity.Wholesaler;
import com.warehouseManagement.demo.repo.OfferRepository;
import com.warehouseManagement.demo.repo.StoreRepository;
import com.warehouseManagement.demo.repo.WholesalerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;


@Service
public class OfferService {
    @Autowired
    WholesalerRepository wholesalerRepository;
    @Autowired
    OfferRepository offerRepository;
    @Autowired
    CartService cartService;;


    public List<Offer> getAllActiveOffers(){
        return offerRepository.findByState(OfferState.active);
    }

    /*
    * When wholesaler wants to change status, we also have to remove offers from carts, we don't want any store to have archived offer in the basket
    * */
    @Transactional
    public void changeOfferState(int userId,int offerId,OfferState desiredState){
        Wholesaler wholesaler = wholesalerRepository.findByUser_Id(userId);
        if(!offerRepository.existsByIdAndWholesaler(offerId,wholesaler)){
            throw new RuntimeException("Offer does not exist or you are not the seller of that offer");
        }

        //We are sure at this point that this offer exists and belongs to the userId
        Offer offer = offerRepository.findById(offerId);
        System.out.println("Wholesaler wants to change state from"+ offer.getState()+" to "+desiredState.toString());
        //we should change status in database and remove it from all carts
        //we delete offer from carts only when we change to somethind different than active
        if(desiredState!=OfferState.active){}
            cartService.removeOfferFromCarts(offer);
        offer.setState(desiredState);
        offerRepository.save(offer);


    }

    @Transactional
    public void changeOffer(int userId, int offerId, BigDecimal price, int availableQuantity, int minimalQuantity) {
        Wholesaler wholesaler = wholesalerRepository.findByUser_Id(userId);
        if(!offerRepository.existsByIdAndWholesaler(offerId,wholesaler)){
            throw new RuntimeException("Offer does not exist or you are not the seller of that offer");
        }

        //We are sure at this point that this offer exists and belongs to the userId
        Offer offer = offerRepository.findById(offerId);
        offer.setPrice(price);
        offer.setAvailable_quantity(availableQuantity);
        offer.setMinimal_quantity(minimalQuantity);



        offerRepository.save(offer);

//        System.out.println("Parameters received -> userId: " + userId +
//                ", offerId: " + offerId +
//                ", price: " + price +
//                ", availableQuantity: " + availableQuantity +
//                ", minimalQuantity: " + minimalQuantity);
    }
}
