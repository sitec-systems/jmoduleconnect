# About

**jModuleConnect** is an framework for communication and file management on modem modules.

The framework supports the AT command set for configuration and controlling modems 
and the OBEX protocol for access to flash filesystem. The adding of further protocol 
processsor is possible for devices the provides more then the AT command set on one 
port.

Filesystem access is optimized for **[Gemalto Cinterion modems](http://m2m.gemalto.com/)**.

**more documentation is available on overview page of javaDoc**

# License

[LGPLv3](http://www.gnu.org/licenses/lgpl.html)

This project was inspired by the project TC65SH by Christoph Vilsmeier

Copyright (C) 2015 sitec systems GmbH

# Example

Reads module information and prints the file listing of the connected module

```java
try
{
    final CommPortIdentifier commPortIdentifier = CommPortIdentifier.getPortIdentifier("COM4");

    try(final CommHandler commHandler = CommHandlerImpl.createCommHandler(commPortIdentifier, 115200); final At at = AtImpl.createAt(commHandler);)
    {
        System.out.println(at.send("ATI"));

        try(final FileManager fileManager = ModuleFileManager.createFileManager(commHandler, at);)
        {
            for(final FileMeta fileMeta: fileManager.getFileListing())
            {
                System.out.println(fileMeta);
            }
        }
    }
    catch (AtCommandFailedException ex)
    {
        Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
    }
    catch (PortInUseException ex)
    {
        Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
    }
    catch (IOException ex)
    {
        Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
    }
}
catch (NoSuchPortException ex)
{
    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
}
}
```
