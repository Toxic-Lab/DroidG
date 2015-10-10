package phex.common.log;

import phex.common.PhexVersion;
import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.LoggingEvent;

public class PhexVersionConverter extends ClassicConverter
{   
    public PhexVersionConverter()
    {
    }

    @Override
    public String convert(LoggingEvent arg0)
    {
        return PhexVersion.getBuild();
    }
}