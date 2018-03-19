package org.sample.coder;

import org.osgi.service.component.annotations.Component;

@Component (service = MyService.class,immediate = true)
public class MyService
{
    public String magic() {
        return 44 + "";
    }
}
