package org.tablerocket.example;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

@Component (immediate = true)
public class BookingService
{
    @Activate
    public void uponActivation() {
        System.out.println("Hello World!");
    }
}
