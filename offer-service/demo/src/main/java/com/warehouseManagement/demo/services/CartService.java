package com.warehouseManagement.demo.services;

import com.warehouseManagement.demo.Exceptions.CartIsNotYoursException;
import com.warehouseManagement.demo.dto.CartItemDTO;
import com.warehouseManagement.demo.dto.OfferPurchaseDTO;
import com.warehouseManagement.demo.dto.CartInformationDTO;

import com.warehouseManagement.demo.entity.*;
import com.warehouseManagement.demo.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor // Automatyczny konstruktor dla repository (wymaga Lombok)
public class CartService {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final OfferRepository offerRepository;
    private final CartRepository cartRepository;
    private final CartOfferRepository cartOfferRepository;


    public List<CartInformationDTO> getCartDetailsForStore(int userId) {
        Store storeEntity = storeRepository.findByUser_Id(userId);
        List<CartInformationDTO> cartList = new ArrayList<>();
        List<Cart> allCarts = cartRepository.findByStore(storeEntity);

        for (Cart cart : allCarts) {
            CartInformationDTO dto = new CartInformationDTO();
            dto.setCartId(cart.getCartId());
            dto.setStoreId(cart.getStore().getId());
            dto.setWholesalerName(cart.getWholesaler().getName());

            BigDecimal cartSum = BigDecimal.ZERO;
            List<CartItemDTO> items = new ArrayList<>();

            // Fetch items for this specific cart
            List<CartOffer> cartOffers = cartOfferRepository.findAllByCart_CartId(cart.getCartId());

            for (CartOffer cartOffer : cartOffers) {
                CartItemDTO item = new CartItemDTO();
                item.setOfferId(cartOffer.getOffer().getId());
                item.setName(cartOffer.getOffer().getProduct().getName());
                item.setQuantity(cartOffer.getQuantity());
                item.setUnitPrice(cartOffer.getOffer().getPrice());

                BigDecimal lineTotal = cartOffer.getOffer().getPrice()
                        .multiply(new BigDecimal(cartOffer.getQuantity()));
                System.out.println("lineTotal: " + lineTotal);
                item.setLineTotal(lineTotal);

                items.add(item);
                cartSum = cartSum.add(lineTotal);
                System.out.println("cartSum: " + cartSum);
            }

            dto.setItems(items);
            dto.setCartTotal(cartSum);

            // Only add to the list if the cart isn't empty
            if (!items.isEmpty()) {
                cartList.add(dto);
            }
        }
        return cartList;
    }


    @Transactional
    public void updateOfferQuantity(int userId,int cartId, int offerId, int newQuantity) {

        Store store = storeRepository.findByUser_Id(userId);
        //If the given cart doesn't belong to the user we should throw an error, we don't want anyone to change other peoples' carts
        if(!cartRepository.existsByCartIdAndStore_Id(cartId, store.getId())){
            throw new CartIsNotYoursException("You are changing someone else's shopping cart");
        }




        // 1. Szukamy konkretnej pozycji po ID koszyka i ID oferty
        CartOffer cp = cartOfferRepository
                .findByCart_CartIdAndOffer_Id(cartId, offerId)
                .orElseThrow(() -> new RuntimeException("Item not found in cart"));



        // 2. Pobieramy ofertę do walidacji (mamy ją w obiekcie cp)
        Offer offer = cp.getOffer();

        // 3. Logika usuwania lub aktualizacji
        if (newQuantity <= 0) {
            cartOfferRepository.delete(cp);
        } else {
            // Walidacja
            if (newQuantity < offer.getMinimal_quantity()) {
                throw new RuntimeException("Minimal quantity is " + offer.getMinimal_quantity());
            }
            if (newQuantity > offer.getAvailable_quantity()) {
                throw new RuntimeException("Not enough stock");
            }

            cp.setQuantity(newQuantity);
            cartOfferRepository.save(cp);
        }
    }

    @Transactional
    public void addOfferToCart(int userId, OfferPurchaseDTO purchaseDTO) {
        // 1. Pobranie danych
        User user = userRepository.findById(userId);
        Store store = storeRepository.findByUser_Id(user.getId());
        Offer offer = offerRepository.getReferenceById(purchaseDTO.getOfferId());

        // 2. Walidacja
        if (purchaseDTO.getQuantity() < offer.getMinimal_quantity()
                || purchaseDTO.getQuantity() > offer.getAvailable_quantity()) {
            throw new RuntimeException("Not enough quantity");
        }

        // 3. Znalezienie lub utworzenie koszyka
        Cart cartEntity = cartRepository.findByStore_IdAndWholesaler_Id(
                store.getId(),
                offer.getWholesaler().getId()
        ).stream().findFirst().orElseGet(() -> {
            Cart newCart = new Cart();
            newCart.setStore(store);
            newCart.setWholesaler(offer.getWholesaler());
            return cartRepository.save(newCart);
        });

        // 4. Aktualizacja produktów w koszyku
        CartOffer cp = cartOfferRepository
                .findByCart_CartIdAndOffer_Id(cartEntity.getCartId(), offer.getId())
                .orElseGet(() -> {
                    CartOffer x = new CartOffer();
                    x.setCart(cartEntity);
                    x.setOffer(offer);
                    x.setQuantity(0);
                    return x;
                });



        cp.setQuantity(cp.getQuantity() + purchaseDTO.getQuantity());
        cartOfferRepository.save(cp);
    }

    //
    public void removeOfferFromCarts(Offer offer) {
        cartOfferRepository.deleteByOffer_Id(offer.getId());
    }

    public boolean clearCarts(List<Integer>cartIds) {

        Cart cart;
        //We iterate over the ids of carts that we want to remove
        for(Integer cartId : cartIds){
            cart = cartRepository.findById(cartId).orElseThrow(() -> new CartIsNotYoursException("Cart not found"));
            for(CartOffer cartOffer : cartOfferRepository.findAllByCart_CartId(cartId)) {
                cartOfferRepository.delete(cartOffer);
            }
            cartRepository.delete(cart);
        }
        System.out.println("Carts have been cleared");

        return true;
    }

}