package org.tablerocket.example.mytest;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.tablerocket.example.calculator.api.Calculator;
import org.tablerocket.febo.api.FeboEntrypoint;
import org.tablerocket.febo.autobundle.api.AutoBundle;

import java.io.InputStream;
import java.io.PrintStream;

@AutoBundle
@Component
public class MyTestEntry implements FeboEntrypoint
{
    private Calculator calc;

    @Activate
    public MyTestEntry(@Reference Calculator calc) {
        this.calc = calc;
    }

    @Override public void execute( String[] args, InputStream in, PrintStream out, PrintStream err )
    {
        if (calc.sum( 1,2 ) != 3) {
            throw new IllegalStateException( "Fail!" );
        }
    }
}
