package com.releasepilot;
import org.springframework.stereotype.Service;
@Service public class QuoteService {
 public record Quote(String sku,int quantity,int unitPrice,int total,String currency){}
 public Quote quote(int quantity){if(quantity<1||quantity>20)throw new IllegalArgumentException("Quantity must be between 1 and 20");return new Quote("LAB-KIT",quantity,499,Math.multiplyExact(499,quantity),"INR");}
}
