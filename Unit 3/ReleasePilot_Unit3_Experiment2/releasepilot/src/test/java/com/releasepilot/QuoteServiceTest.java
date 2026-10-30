package com.releasepilot;
import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;
class QuoteServiceTest {
 private final QuoteService service=new QuoteService();
 @Test void totalUsesQuantity(){assertEquals(998,service.quote(2).total());assertEquals("INR",service.quote(2).currency());}
 @Test void acceptsBoundaryQuantities(){assertEquals(499,service.quote(1).total());assertEquals(9980,service.quote(20).total());}
 @Test void rejectsInvalidQuantities(){assertThrows(IllegalArgumentException.class,()->service.quote(0));assertThrows(IllegalArgumentException.class,()->service.quote(21));}
 @Test void routingIsSticky(){for(int i=0;i<100;i++)assertEquals(RoutingPolicy.select("visitor-"+i,10),RoutingPolicy.select("visitor-"+i,10));}
 @Test void routingBoundaries(){for(int i=0;i<100;i++){assertEquals("stable",RoutingPolicy.select("v"+i,0));assertEquals("canary",RoutingPolicy.select("v"+i,100));}}
 @Test void tenPercentCohortIsApproximatelyCorrect(){int canary=0;for(int i=0;i<10000;i++)if(RoutingPolicy.select("visitor-"+i,10).equals("canary"))canary++;assertTrue(canary>800&&canary<1200);}
}
